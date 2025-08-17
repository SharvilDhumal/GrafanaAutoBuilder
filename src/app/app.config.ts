import { ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners, provideZoneChangeDetection, ErrorHandler, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptorsFromDi } from '@angular/common/http';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { MatDialogModule } from '@angular/material/dialog';
import { GlobalErrorHandler } from './core/errors/global-error.handler';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpErrorInterceptor } from './core/errors/http-error.interceptor';
import { AuthService } from './services/auth.service';
import { AuthTokenInterceptor } from './core/auth/auth-token.interceptor';

function initAuth(auth: AuthService) {
  return () => auth.initialize();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes), 
    provideClientHydration(withEventReplay()),
    provideAnimations(),
    // Make MatDialog service available app-wide
    importProvidersFrom(MatDialogModule),
    provideHttpClient(withFetch(), withInterceptorsFromDi()),
    // Global error handler
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    // Attach JWT automatically to outgoing requests
    { provide: HTTP_INTERCEPTORS, useClass: AuthTokenInterceptor, multi: true },
    // HTTP error interceptor
    { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true },
    // Ensure auth rehydration BEFORE router/guards run on initial load
    { provide: APP_INITIALIZER, useFactory: initAuth, deps: [AuthService], multi: true }
  ]
};
