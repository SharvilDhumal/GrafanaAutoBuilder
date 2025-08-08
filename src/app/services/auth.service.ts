import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, catchError, throwError } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

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
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  // Login method
  login(loginRequest: LoginRequest): Observable<AuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, loginRequest, { 
      headers,
      withCredentials: true 
    }).pipe(
      tap(response => {
        if (response.token && isPlatformBrowser(this.platformId)) {
          localStorage.setItem('authToken', response.token);
          localStorage.setItem('tokenExpiry', response.expiresAt);
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
      localStorage.removeItem('authToken');
      localStorage.removeItem('tokenExpiry');
      this.isAuthenticatedSubject.next(false);
    }
    this.router.navigate(['/login']);
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }
    
    const token = localStorage.getItem('authToken');
    const expiry = localStorage.getItem('tokenExpiry');
    
    if (!token || !expiry) {
      return false;
    }
    
    const expiryDate = new Date(expiry);
    const now = new Date();
    
    if (expiryDate < now) {
      this.logout();
      return false;
    }
    
    return true;
  }
}
