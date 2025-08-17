import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ErrorDialogComponent, ErrorDialogData } from '../../shared/error-dialog/error-dialog.component';

@Injectable({ providedIn: 'root' })
export class GlobalErrorService {
  constructor(private dialog: MatDialog) {}

  showError(data: ErrorDialogData) {
    this.dialog.open(ErrorDialogComponent, {
      data,
      width: '560px',
      maxWidth: '90vw',
      disableClose: false
    });
  }

  showHttpError(status: number | undefined, message: string, details?: string) {
    let title = 'Error';
    if (status === 401) title = 'Sign in required';
    else if (status === 403) title = 'Access denied';
    else if (status === 400) title = 'Bad request';
    else if (status && status >= 500) title = 'Server error';

    this.showError({ title, message, details, statusCode: status });
  }
}
