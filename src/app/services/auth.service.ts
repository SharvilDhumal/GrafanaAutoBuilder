import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';

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

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
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
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, loginRequest, { headers })
      .pipe(
        tap(response => {
          if (response.token && isPlatformBrowser(this.platformId)) {
            localStorage.setItem('authToken', response.token);
            localStorage.setItem('tokenExpiry', response.expiresAt);
            this.isAuthenticatedSubject.next(true);
          }
        })
      );
  }

  // Signup method
  signup(signupRequest: SignupRequest): Observable<void> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<void>(`${this.apiUrl}/signup`, signupRequest, { headers });
  }

  // Logout method
  logout(): void {
    localStorage.removeItem('authToken');
    localStorage.removeItem('tokenExpiry');
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    // localStorage is not available during server-side rendering
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
    
    // If token is expired, remove it and return false
    if (expiryDate < now) {
      this.logout();
      return false;
    }
    
    return true;
  }
}
