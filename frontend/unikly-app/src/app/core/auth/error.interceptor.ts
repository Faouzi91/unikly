import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error) => {
      // Skip 401 — handled by auth interceptor
      if (error.status === 401) {
        return throwError(() => error);
      }

      let message = 'An unexpected error occurred';

      if (error.status >= 400 && error.status < 500) {
        message = error.error?.message || error.error?.error || 'Request failed';
      } else if (error.status >= 500) {
        message = 'Server error. Please try again later.';
      } else if (error.status === 0) {
        message = 'Unable to connect to server';
      }

      snackBar.open(message, 'Close', {
        duration: 5000,
        horizontalPosition: 'end',
        verticalPosition: 'top',
        panelClass: error.status >= 500 ? 'error-snackbar' : 'warning-snackbar',
      });

      return throwError(() => error);
    })
  );
};
