import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { GlobalErrorService } from '../core/errors/global-error.service';

// Allows access only when authenticated AND email === admin@gmail.com
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const errors = inject(GlobalErrorService);
  const isAuthed = auth.isAuthenticated();
  const email = auth.getCurrentEmail()?.toLowerCase().trim();

  if (isAuthed && email === 'admin@gmail.com') {
    return true;
  }

  // Not allowed: redirect to login for unauth or home for non-admin
  if (!isAuthed) {
    errors.showHttpError(401, 'You need to sign in to access this page.');
    router.navigate(['/login']);
  } else {
    errors.showHttpError(403, 'You do not have permission to access this page.');
    router.navigate(['/']);
  }
  return false;
};
