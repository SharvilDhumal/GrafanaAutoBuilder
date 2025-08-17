import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, catchError, throwError } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';
import { MetricsService } from './metrics.service';

// Define interfaces for our API responses
export interface AuthResponse {
  token: string;
  expiresAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  token: string;
  newPassword: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
    private metrics: MetricsService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    // Also attempt a best-effort restore on service creation
    this.initialize();
  }

  // Extract email/subject from JWT if available
  private getJwtEmail(token: string | null): string | null {
    if (!token) return null;
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
      const sub = payload?.sub || payload?.email;
      return typeof sub === 'string' ? sub : null;
    } catch {
      return null;
    }
  }

  // Safely parse JWT and return expiry in ms since epoch if present
  private getJwtExpiryMs(token: string | null): number | null {
    if (!token) return null;
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
      if (payload && typeof payload.exp === 'number') {
        return payload.exp * 1000;
      }
      return null;
    } catch {
      return null;
    }
  }

  // Rehydrate authentication state on service construction (app reload)
  // If a valid token and email are present in sessionStorage, resume presence
  // and mark the auth subject as true so guards/components see the state immediately.
  public initialize() {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      const token = sessionStorage.getItem('authToken');
      const expiry = sessionStorage.getItem('tokenExpiry') || sessionStorage.getItem('tokenExpiryMs');
      let email = sessionStorage.getItem('email');
      if (!email) {
        const fromJwt = this.getJwtEmail(token);
        if (fromJwt) {
          email = fromJwt.toLowerCase().trim();
          sessionStorage.setItem('email', email);
        }
      }
      try { console.log('[Auth] initialize() session', { hasToken: !!token, expiryRaw: expiry, email }); } catch {}
      if (token && email) {
        // Prefer stored ms; else ISO; else decode from JWT exp
        let ms = expiry ? Number(expiry) : NaN;
        if (isNaN(ms)) {
          const tryIso = expiry ? Date.parse(String(expiry)) : NaN;
          ms = isNaN(tryIso) ? (this.getJwtExpiryMs(token) ?? NaN) : tryIso;
        }
        const expiryDate = !isNaN(ms) ? new Date(ms) : new Date(0);
        try { console.log('[Auth] initialize() parsed expiry', { expiryDate: expiryDate.toISOString(), now: new Date().toISOString() }); } catch {}
        if (expiryDate > new Date()) {
          // Valid session: notify and resume presence without increasing visits
          this.isAuthenticatedSubject.next(true);
          this.metrics.resumePresence(email.toLowerCase().trim());
        } else {
          try { console.log('[Auth] initialize() expired session on load'); } catch {}
        }
      }
    } catch {
      // ignore rehydrate errors
    }
  }

  // Login method
  login(loginRequest: LoginRequest): Observable<AuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, loginRequest, { 
      headers,
      withCredentials: true 
    }).pipe(
      tap(response => {
        if (response.token && isPlatformBrowser(this.platformId)) {
          sessionStorage.setItem('authToken', response.token);
          // Persist expiry in both ISO and epoch ms for robust parsing across browsers
          try {
            let ms = Date.parse(String(response.expiresAt));
            if (isNaN(ms)) {
              const fromJwt = this.getJwtExpiryMs(response.token);
              if (fromJwt) ms = fromJwt;
            }
            if (!isNaN(ms)) {
              sessionStorage.setItem('tokenExpiryMs', String(ms));
            }
          } catch {}
          sessionStorage.setItem('tokenExpiry', String(response.expiresAt));
          // Persist email from the login request so we can identify the user client-side
          if (loginRequest.email) {
            sessionStorage.setItem('email', loginRequest.email.toLowerCase().trim());
            // Count visit and start presence heartbeat for active users metric
            this.metrics.markVisit(loginRequest.email.toLowerCase().trim());
          }
          this.isAuthenticatedSubject.next(true);
        }
      }),
      catchError(this.handleError)
    );
  }

  // Signup method
  signup(signupRequest: SignupRequest): Observable<void> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<void>(`${this.apiUrl}/signup`, signupRequest, { 
      headers,
      withCredentials: true
    }).pipe(
      tap(() => {
        console.log('Signup successful');
      }),
      catchError(this.handleError)
    );
  }

  // Forgot password: triggers email with reset link
  forgotPassword(email: string): Observable<void> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const payload: ForgotPasswordRequest = { email };
    return this.http.post<void>(`${this.apiUrl}/forgot-password`, payload, {
      headers,
      withCredentials: true
    }).pipe(
      tap(() => console.log('Forgot password email sent (if account exists)')),
      catchError(this.handleError)
    );
  }

  // Reset password: submits the token, email, and new password
  resetPassword(email: string, token: string, newPassword: string): Observable<void> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const payload: ResetPasswordRequest = { email, token, newPassword };
    return this.http.post<void>(`${this.apiUrl}/reset-password`, payload, {
      headers,
      withCredentials: true
    }).pipe(
      tap(() => console.log('Password reset successful')),
      catchError(this.handleError)
    );
  }

  // Handle errors
  private handleError(error: HttpErrorResponse) {
    let message = 'An unknown error occurred';
    if (error.error instanceof ErrorEvent) {
      // Client-side/network error
      message = error.error.message || message;
    } else {
      // Server-side error: prefer backend-provided message (often plain text)
      if (typeof error.error === 'string' && error.error.trim().length > 0) {
        message = error.error.trim();
      } else if (error.error && error.error.message) {
        message = error.error.message;
      } else if (error.message) {
        message = error.message;
      }
    }
    console.error('Auth API error', { status: error.status, message, raw: error });
    // Return a lightweight error-like object with status and message
    return throwError(() => ({ status: error.status, message }));
  }

  // Logout method
  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem('authToken');
      sessionStorage.removeItem('tokenExpiry');
      const email = sessionStorage.getItem('email');
      if (email) {
        this.metrics.clearPresence();
      }
      sessionStorage.removeItem('email');
      this.isAuthenticatedSubject.next(false);
    }
    this.router.navigate(['/login']);
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }
    
    const token = sessionStorage.getItem('authToken');
    const expiryIso = sessionStorage.getItem('tokenExpiry');
    let expiryMsStr = sessionStorage.getItem('tokenExpiryMs');
    
    // Minimal debug to understand guard path
    try { console.log('[Auth] isAuthenticated() keys', { hasToken: !!token, expiryIso, expiryMsStr }); } catch {}
    
    if (!token) return false;
    if (!expiryIso && !expiryMsStr) {
      // Fallback to JWT exp if no stored expiry values
      const fromJwt = this.getJwtExpiryMs(token);
      if (fromJwt) {
        expiryMsStr = String(fromJwt);
      } else {
        // If token exists but no expiry available, optimistically treat as authenticated
        // (HTTP interceptor will enforce 401 on actual API calls)
        return true;
      }
    }
    
    let expiryDate: Date;
    if (expiryMsStr && !isNaN(Number(expiryMsStr))) {
      expiryDate = new Date(Number(expiryMsStr));
    } else {
      const parsed = expiryIso ? Date.parse(String(expiryIso)) : NaN;
      if (!isNaN(parsed)) {
        expiryDate = new Date(parsed);
      } else {
        // Last fallback: decode JWT exp
        const fromJwt = this.getJwtExpiryMs(token);
        if (fromJwt) {
          expiryDate = new Date(fromJwt);
        } else {
          return false;
        }
      }
    }
    const now = new Date();
    try { console.log('[Auth] isAuthenticated() compare', { expiry: expiryDate.toISOString(), now: now.toISOString() }); } catch {}
    
    if (expiryDate < now) return false;
    
    return true;
  }

  // Get current user's email stored at login time
  getCurrentEmail(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    const email = sessionStorage.getItem('email');
    return email ? email : null;
  }
}
