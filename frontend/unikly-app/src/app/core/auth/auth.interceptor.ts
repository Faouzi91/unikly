import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap, catchError, throwError, BehaviorSubject, filter, take } from 'rxjs';
import { KeycloakService } from './keycloak.service';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const keycloak = inject(KeycloakService);

  if (
    !req.url.includes('/api/') ||
    req.url.includes('/api/users/register') ||
    req.url.includes('/api/users/login') ||
    req.url.includes('/api/users/refresh') ||
    req.url.includes('/api/users/social/')
  ) {
    return next(req);
  }

  return from(keycloak.getToken()).pipe(
    switchMap((token) => {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      });
      return next(authReq).pipe(
        catchError((error) => {
          if (error instanceof HttpErrorResponse && error.status === 401) {
            return handle401Error(req, next, keycloak, error);
          }
          return throwError(() => error);
        })
      );
    })
  );
};

const handle401Error = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  keycloak: KeycloakService,
  fallbackError: unknown
) => {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return from(keycloak.refreshTokens()).pipe(
      switchMap((newToken) => {
        isRefreshing = false;
        if (newToken) {
          refreshTokenSubject.next(newToken);
          return next(
            req.clone({
              setHeaders: {
                Authorization: `Bearer ${newToken}`,
              },
            })
          );
        }
        keycloak.login();
        return throwError(() => fallbackError);
      }),
      catchError((err) => {
        isRefreshing = false;
        keycloak.login();
        return throwError(() => err);
      })
    );
  } else {
    return refreshTokenSubject.pipe(
      filter((token) => token !== null),
      take(1),
      switchMap((token) => {
        if (!token) {
          return throwError(() => fallbackError);
        }
        return next(
          req.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`,
            },
          })
        );
      })
    );
  }
};
