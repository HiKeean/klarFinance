import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { from, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AuthStateService } from '../services/auth-state.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);
  if (request.url.includes('/auth/login') || request.url.includes('/auth/password-reset-requests')) return next(request);
  return from(authState.getAccessToken()).pipe(
    switchMap(async (token) => {
      if (!token || !(await authState.isSessionValid())) {
        await authState.notifySessionExpired();
        await router.navigateByUrl('/reauth');
        throw new Error('SESSION_EXPIRED');
      }
      return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
    }),
    switchMap((request$) => request$),
    catchError((error) => {
      if (error?.status === 401 || error?.status === 403 || error?.message === 'SESSION_EXPIRED') {
        void authState.notifySessionExpired();
        void router.navigateByUrl('/reauth');
      }
      return throwError(() => error);
    })
  );
};
