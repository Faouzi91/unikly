import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

/** URLs that should NOT trigger the global loading spinner. */
const SILENT_PATTERNS = [
  '/v1/notifications',    // background polling / websocket fallback
  '/api-docs',            // Swagger spec fetches
];

export const loadingInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
) => {
  const loading = inject(LoadingService);

  // Skip spinner for background fetches
  if (SILENT_PATTERNS.some((p) => req.url.includes(p))) {
    return next(req);
  }

  loading.start();
  return next(req).pipe(finalize(() => loading.stop()));
};
