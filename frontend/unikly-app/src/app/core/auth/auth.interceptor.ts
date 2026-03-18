import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap, catchError, throwError } from 'rxjs';
import { KeycloakService } from './keycloak.service';

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const keycloak = inject(KeycloakService);

  if (!req.url.includes('/api/')) {
    return next(req);
  }

  return from(keycloak.getToken()).pipe(
    switchMap((token) => {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      });
      return next(authReq);
    }),
    catchError((error) => {
      if (error.status === 401) {
        return from(keycloak.getToken()).pipe(
          switchMap((newToken) => {
            const retryReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newToken}`,
              },
            });
            return next(retryReq);
          }),
          catchError(() => {
            keycloak.login();
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
