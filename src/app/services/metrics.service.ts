import { Injectable, NgZone, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, interval } from 'rxjs';

export interface ActivityItem {
  user: string;
  docName: string;
  action: 'converted';
  timestamp: number; // epoch ms
}

// Keys used in localStorage for cross-tab metrics
const LS_KEYS = {
  totalVisits: 'metrics.totalVisits',
  presence: 'metrics.presence', // JSON map: { [email]: lastSeenMs }
  activities: 'metrics.activities' // JSON array of ActivityItem, newest first
};

const HEARTBEAT_MS = 10000; // 10s heartbeat for fast detection on other tabs
const STALE_AFTER_MS = 2 * 60 * 60 * 1000; // 2 hours grace; considered offline sooner only if tab signals close
const MAX_ACTIVITIES = 100;

@Injectable({ providedIn: 'root' })
export class MetricsService {
  totalVisits$ = new BehaviorSubject<number>(0);
  activeUsers$ = new BehaviorSubject<number>(0);
  activities$ = new BehaviorSubject<ActivityItem[]>([]);

  private email: string | null = null;
  private sid: string | null = null; // per-tab session id
  private bc?: BroadcastChannel;
  private readonly browser: boolean;

  constructor(private zone: NgZone, @Inject(PLATFORM_ID) platformId: Object) {
    this.browser = isPlatformBrowser(platformId);
    // Create or reuse a per-tab session id
    if (this.browser) {
      try {
        const existing = sessionStorage.getItem('metrics.sid');
        this.sid = existing || this.generateSid();
        sessionStorage.setItem('metrics.sid', this.sid);
      } catch {
        // sessionStorage might be unavailable; fallback to random
        this.sid = this.generateSid();
      }
    }
    // Initialize from localStorage
    this.totalVisits$.next(this.getTotalVisits());
    this.activities$.next(this.getActivities());
    this.computeActiveUsers();

    // Listen for cross-tab updates
    if (this.browser) {
      window.addEventListener('storage', () => {
        this.totalVisits$.next(this.getTotalVisits());
        this.activities$.next(this.getActivities());
        this.computeActiveUsers();
      });
      // Ensure prompt decrement on tab close
      const markStaleNow = () => this.markStaleImmediately();
      // Use both events for broader browser coverage
      window.addEventListener('beforeunload', markStaleNow);
      window.addEventListener('pagehide', markStaleNow as any);
    }

    // Optional BroadcastChannel for more immediate sync
    if (this.browser && 'BroadcastChannel' in window) {
      this.bc = new BroadcastChannel('metrics');
      this.bc.onmessage = (e) => {
        if (e.data?.type === 'presence' || e.data?.type === 'activity' || e.data?.type === 'visits') {
          this.totalVisits$.next(this.getTotalVisits());
          this.activities$.next(this.getActivities());
          this.computeActiveUsers();
        }
      };
    }

    // Periodic sweep to expire stale users and refresh observers even without events
    if (this.browser) {
      this.zone.runOutsideAngular(() => {
        setInterval(() => this.computeActiveUsers(), 5000);
      });
    }
  }

  // Call on successful login with user email
  markVisit(email: string) {
    this.email = email;
    // Increment total visits atomically-ish
    const current = this.getTotalVisits();
    if (this.browser) {
      localStorage.setItem(LS_KEYS.totalVisits, String(current + 1));
    }
    this.totalVisits$.next(current + 1);
    this.bc?.postMessage({ type: 'visits' });

    // Start presence heartbeat
    this.startPresenceHeartbeat();
  }

  // Call on logout
  clearPresence() {
    if (!this.email) return;
    const map = this.getPresenceMap();
    // Remove ALL sessions for this email (not just this tab)
    const email = this.email;
    for (const k of Object.keys(map)) {
      if (k === email || k.startsWith(email + '#')) {
        delete map[k];
      }
    }
    if (this.browser) {
      localStorage.setItem(LS_KEYS.presence, JSON.stringify(map));
    }
    this.bc?.postMessage({ type: 'presence' });
    this.computeActiveUsers();
    this.email = null;
  }

  // Record a conversion activity
  recordConversion(user: string, docName: string) {
    const list = this.getActivities();
    const item: ActivityItem = { user, docName, action: 'converted', timestamp: Date.now() };
    const next = [item, ...list].slice(0, MAX_ACTIVITIES);
    if (this.browser) {
      localStorage.setItem(LS_KEYS.activities, JSON.stringify(next));
    }
    this.activities$.next(next);
    this.bc?.postMessage({ type: 'activity' });
  }

  // Internal helpers
  private startPresenceHeartbeat() {
    if (!this.email || !this.browser) return;
    const setBeat = () => {
      const map = this.getPresenceMap();
      const key = this.presenceKey();
      if (key) {
        map[key] = Date.now();
      }
      if (this.browser) {
        localStorage.setItem(LS_KEYS.presence, JSON.stringify(map));
      }
      this.bc?.postMessage({ type: 'presence' });
      this.computeActiveUsers();
    };
    // First beat immediately
    setBeat();
    // Heartbeat in zone to ensure change detection if needed
    this.zone.runOutsideAngular(() => {
      const sub = interval(HEARTBEAT_MS).subscribe(() => setBeat());
      // We intentionally don't store sub; page lifecycle will clean it up
    });
  }

  private computeActiveUsers() {
    const map = this.getPresenceMap();
    const now = Date.now();
    const unique = new Set<string>();
    let mutated = false;
    for (const [key, ts] of Object.entries(map)) {
      const last = Number(ts);
      if (!isFinite(last) || now - last > STALE_AFTER_MS) {
        // Prune stale/invalid entries so they don't linger for hours/days
        delete map[key as string];
        mutated = true;
        continue;
      }
      const email = key.includes('#') ? key.split('#')[0] : key; // support legacy keys
      unique.add(email);
    }
    if (mutated && this.browser) {
      localStorage.setItem(LS_KEYS.presence, JSON.stringify(map));
      this.bc?.postMessage({ type: 'presence' });
    }
    this.activeUsers$.next(unique.size);
  }

  private getTotalVisits(): number {
    if (!this.browser) return 0;
    const s = localStorage.getItem(LS_KEYS.totalVisits);
    return s ? Number(s) || 0 : 0;
    }

  private getPresenceMap(): Record<string, number> {
    if (!this.browser) return {};
    try {
      const s = localStorage.getItem(LS_KEYS.presence);
      return s ? JSON.parse(s) : {};
    } catch {
      return {};
    }
  }

  private getActivities(): ActivityItem[] {
    if (!this.browser) return [];
    try {
      const s = localStorage.getItem(LS_KEYS.activities);
      return s ? JSON.parse(s) as ActivityItem[] : [];
    } catch {
      return [];
    }
  }

  // Mark this user's presence as immediately stale (used on tab close)
  private markStaleImmediately() {
    if (!this.browser || !this.email) return;
    try {
      const map = this.getPresenceMap();
      const key = this.presenceKey();
      // Set last seen to a time older than the stale threshold so other tabs drop it promptly
      if (key) map[key] = Date.now() - (STALE_AFTER_MS + 1000);
      localStorage.setItem(LS_KEYS.presence, JSON.stringify(map));
      this.bc?.postMessage({ type: 'presence' });
    } catch {
      // noop
    }
  }

  // Build the presence map key combining email and per-tab session id
  private presenceKey(): string | null {
    if (!this.email || !this.sid) return null;
    return `${this.email}#${this.sid}`;
  }

  // Simple SID generator; prefer crypto if available
  private generateSid(): string {
    try {
      const arr = new Uint8Array(8);
      (window.crypto || (window as any).msCrypto).getRandomValues(arr);
      return Array.from(arr).map(b => b.toString(16).padStart(2, '0')).join('');
    } catch {
      return Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2);
    }
  }
}
