// centralizes HTTP and global error handling via interceptors and a global error handler.
// Catches HTTP errors globally and displays user-friendly dialogs via 
// GlobalErrorService



import { ErrorHandler, Injectable, NgZone } from '@angular/core';
import { GlobalErrorService } from './global-error.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  constructor(private errors: GlobalErrorService, private zone: NgZone) {}

  handleError(error: any): void {
    // Run inside Angular zone to ensure dialog opens properly
    this.zone.run(() => {
      const message = (error?.message || error?.toString || 'Unexpected error').toString();
      const stack = error?.stack;
      this.errors.showError({
        title: 'Unexpected error',
        message,
        details: stack
      });
    });

    // Optionally rethrow or log
    // console.error(error);
  }
}
