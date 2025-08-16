import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Allows access only when authenticated AND email === admin@gmail.com
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const isAuthed = auth.isAuthenticated();
  const email = auth.getCurrentEmail()?.toLowerCase().trim();

  if (isAuthed && email === 'admin@gmail.com') {
    return true;
  }

  // Not allowed: redirect to login for unauth or home for non-admin
  if (!isAuthed) {
    router.navigate(['/login']);
  } else {
    router.navigate(['/']);
  }
  return false;
};
