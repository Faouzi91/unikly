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

      const body = error.error;
      let message = 'An unexpected error occurred';

      switch (error.status) {
        case 400: {
          const details: Array<{ field: string; message: string }> = body?.details;
          message = details?.length > 0
            ? `${details[0].field}: ${details[0].message}`
            : body?.message || 'Invalid input';
          break;
        }
        case 403:
          message = "You don't have permission for this action";
          break;
        case 404:
          message = 'Resource not found';
          break;
        case 409:
          message = 'This resource was modified. Please refresh and try again.';
          break;
        case 503:
          message = 'Service temporarily unavailable. Please try again.';
          break;
        case 500: {
          const traceId: string = body?.traceId;
          message = traceId
            ? `Something went wrong. Please try again later. (Ref: ${traceId})`
            : 'Something went wrong. Please try again later.';
          break;
        }
        default:
          if (error.status === 0) {
            message = 'Unable to connect to server';
          } else if (error.status >= 400 && error.status < 500) {
            message = body?.message || body?.error || 'Request failed';
          } else if (error.status >= 500) {
            message = 'Server error. Please try again later.';
          }
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
