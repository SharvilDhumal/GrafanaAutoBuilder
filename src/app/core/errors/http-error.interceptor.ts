import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { GlobalErrorService } from './global-error.service';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  private readonly suppressStatuses = new Set<number>();

  constructor(private errors: GlobalErrorService, private router: Router, private auth: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((err: any) => {
        if (err instanceof HttpErrorResponse) {
          const status = err.status;
          const backendMessage = typeof err.error === 'string' ? err.error : (err.error?.message || err.message);
          try { console.log('[HttpErrorInterceptor]', { status, url: err.url, method: req.method, backendMessage }); } catch {}

          // Avoid spamming multiple dialogs for the same status in rapid succession
          if (!this.suppressStatuses.has(status)) {
            this.suppressStatuses.add(status);
            setTimeout(() => this.suppressStatuses.delete(status), 1000);

            // Friendly messages per status
            if (status === 0) {
              this.errors.showHttpError(undefined, 'Cannot reach server. Please check your network and try again.');
            } else if (status === 401) {
              // If we already consider the user authenticated (e.g., after refresh),
              // suppress the blocking dialog to avoid confusion. Guards will handle
              // access control; components can handle specific 401s locally.
              const authed = this.auth.isAuthenticated();
              if (!authed) {
                this.errors.showHttpError(401, 'You need to sign in to continue.');
              }
            } else if (status === 403) {
              this.errors.showHttpError(403, 'You do not have permission to perform this action.');
            } else if (status === 400) {
              this.errors.showHttpError(400, backendMessage || 'The request was invalid.');
            } else if (status >= 500) {
              this.errors.showHttpError(status, 'The server encountered an error. Please try again later.', backendMessage);
            } else {
              this.errors.showHttpError(status, backendMessage || 'Request failed.');
            }
          }
        }
        return throwError(() => err);
      })
    );
  }
}
