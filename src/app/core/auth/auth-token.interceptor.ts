import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthTokenInterceptor implements HttpInterceptor {
  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Only run in browser
    const isBrowser = typeof window !== 'undefined' && typeof sessionStorage !== 'undefined';
    if (!isBrowser) {
      return next.handle(req);
    }

    const token = sessionStorage.getItem('authToken');
    if (!token) {
      return next.handle(req);
    }

    // Avoid adding header to auth endpoints if not desired
    const url = req.url || '';
    const isAuthEndpoint = /\/(login|signup|verify|reset-password)/i.test(url);
    if (isAuthEndpoint) {
      return next.handle(req);
    }

    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
      withCredentials: true,
    });

    try {
      // Lightweight visibility to confirm header is present on first calls after refresh
      // Remove or lower level if too noisy
      console.log('[AuthTokenInterceptor] Attached Authorization header', { url });
    } catch {}

    return next.handle(cloned);
  }
}
