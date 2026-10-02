import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { API_URL } from '../../app.constants';
import { AuthService } from '../auth.service';

export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/auth/') && !request.url.startsWith('/api/')) return next(request);
  const http = inject(HttpClient);
  const auth = inject(AuthService);
  const router = inject(Router);
  const authenticated = request.clone({
    withCredentials: true,
    setHeaders: request.url.startsWith('/api/')
      ? { 'X-Time-Zone': Intl.DateTimeFormat().resolvedOptions().timeZone }
      : {},
  });
  const safe = ['GET', 'HEAD', 'OPTIONS'].includes(request.method);
  const result = safe
    ? next(authenticated)
    : http
        .get<{ headerName: string; token: string }>(`${API_URL}/auth/csrf`, {
          withCredentials: true,
        })
        .pipe(
          switchMap((csrf) =>
            next(
              authenticated.clone({
                setHeaders: { [csrf.headerName]: csrf.token },
              }),
            ),
          ),
        );
  return result.pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !request.url.endsWith('/auth/login')) {
        auth.clearSession();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
