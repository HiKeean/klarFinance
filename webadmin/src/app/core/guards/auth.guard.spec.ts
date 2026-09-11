import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { authGuard, loginGuard } from './auth.guard';
import { AuthStateService } from '../services/auth-state.service';

describe('auth.guard', () => {
  let authState: jasmine.SpyObj<AuthStateService>;
  let router: Router;

  beforeEach(() => {
    authState = jasmine.createSpyObj<AuthStateService>('AuthStateService', [
      'getAccessToken',
      'isSessionValid',
      'notifySessionExpired'
    ]);

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStateService, useValue: authState }]
    });
    router = TestBed.inject(Router);
  });

  function runAuthGuard() {
    return TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
  }

  function runLoginGuard() {
    return TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));
  }

  describe('authGuard', () => {
    it('[negative] redirects to /login when there is no access token', async () => {
      authState.getAccessToken.and.resolveTo(null);

      const result = await runAuthGuard();

      expect(result instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(result as UrlTree)).toBe('/login');
      expect(authState.isSessionValid).not.toHaveBeenCalled();
    });

    it('[positive] allows navigation when the token exists and the session is valid', async () => {
      authState.getAccessToken.and.resolveTo('a.b.c');
      authState.isSessionValid.and.resolveTo(true);

      const result = await runAuthGuard();

      expect(result).toBeTrue();
    });

    it('[negative] notifies session expiry and redirects to /reauth when the token is expired', async () => {
      authState.getAccessToken.and.resolveTo('a.b.c');
      authState.isSessionValid.and.resolveTo(false);
      authState.notifySessionExpired.and.resolveTo(undefined);

      const result = await runAuthGuard();

      expect(authState.notifySessionExpired).toHaveBeenCalled();
      expect(result instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(result as UrlTree)).toBe('/reauth');
    });
  });

  describe('loginGuard', () => {
    it('[positive] allows access to the login page when there is no token', async () => {
      authState.getAccessToken.and.resolveTo(null);

      const result = await runLoginGuard();

      expect(result).toBeTrue();
    });

    it('[negative] redirects an already-authenticated user away from /login to /dashboard', async () => {
      authState.getAccessToken.and.resolveTo('a.b.c');
      authState.isSessionValid.and.resolveTo(true);

      const result = await runLoginGuard();

      expect(result instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(result as UrlTree)).toBe('/dashboard');
    });

    it('[positive] still allows access to /login when the stale token has an expired session', async () => {
      authState.getAccessToken.and.resolveTo('a.b.c');
      authState.isSessionValid.and.resolveTo(false);

      const result = await runLoginGuard();

      expect(result).toBeTrue();
    });
  });
});
