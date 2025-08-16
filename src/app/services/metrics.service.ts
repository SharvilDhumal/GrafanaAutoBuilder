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

const HEARTBEAT_MS = 10000; // 10s
const STALE_AFTER_MS = 30000; // 30s considered offline
const MAX_ACTIVITIES = 100;

@Injectable({ providedIn: 'root' })
export class MetricsService {
  totalVisits$ = new BehaviorSubject<number>(0);
  activeUsers$ = new BehaviorSubject<number>(0);
  activities$ = new BehaviorSubject<ActivityItem[]>([]);

  private email: string | null = null;
  private bc?: BroadcastChannel;
  private readonly browser: boolean;

  constructor(private zone: NgZone, @Inject(PLATFORM_ID) platformId: Object) {
    this.browser = isPlatformBrowser(platformId);
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
    delete map[this.email];
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
      map[this.email!] = Date.now();
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
    const active = Object.values(map).filter(ts => now - ts <= STALE_AFTER_MS).length;
    this.activeUsers$.next(active);
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
}
