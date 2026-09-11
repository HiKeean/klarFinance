import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';

export const authGuard: CanActivateFn = async () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const token = await authState.getAccessToken();

  if (!token) {
    return router.parseUrl('/login');
  }

  const valid = await authState.isSessionValid();

  if (valid) {
    return true;
  }

  await authState.notifySessionExpired();
  return router.parseUrl('/reauth');
};

export const loginGuard: CanActivateFn = async () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const token = await authState.getAccessToken();

  if (!token) {
    return true;
  }

  const valid = await authState.isSessionValid();

  if (valid) {
    return router.parseUrl('/dashboard');
  }

  return true;
};
