import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { authGuard, loginGuard } from './auth.guard';
import { AuthStateService } from '../services/auth-state.service';

describe('authGuard', () => {
  let authState: { getAccessToken: ReturnType<typeof vi.fn>; isSessionValid: ReturnType<typeof vi.fn>; notifySessionExpired: ReturnType<typeof vi.fn> };
  let router: Router;

  function setup(platform: 'browser' | 'server') {
    authState = { getAccessToken: vi.fn(), isSessionValid: vi.fn(), notifySessionExpired: vi.fn() };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStateService, useValue: authState }, { provide: PLATFORM_ID, useValue: platform }]
    });
    router = TestBed.inject(Router);
  }

  it('[positive] SSR (server platform) bypasses the check and allows activation', async () => {
    setup('server');
    const result = await TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(result).toBe(true);
    expect(authState.getAccessToken).not.toHaveBeenCalled();
  });

  it('[negative] no token redirects to /login', async () => {
    setup('browser');
    authState.getAccessToken.mockResolvedValue(null);
    const result = await TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(router.serializeUrl(result as any)).toBe('/login');
  });

  it('[positive] a valid session allows activation', async () => {
    setup('browser');
    authState.getAccessToken.mockResolvedValue('token');
    authState.isSessionValid.mockResolvedValue(true);
    const result = await TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('[negative] an invalid session notifies expiry and redirects to /reauth', async () => {
    setup('browser');
    authState.getAccessToken.mockResolvedValue('token');
    authState.isSessionValid.mockResolvedValue(false);
    const result = await TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(authState.notifySessionExpired).toHaveBeenCalled();
    expect(router.serializeUrl(result as any)).toBe('/reauth');
  });
});

describe('loginGuard', () => {
  let authState: { getAccessToken: ReturnType<typeof vi.fn>; isSessionValid: ReturnType<typeof vi.fn> };
  let router: Router;

  function setup(platform: 'browser' | 'server') {
    authState = { getAccessToken: vi.fn(), isSessionValid: vi.fn() };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStateService, useValue: authState }, { provide: PLATFORM_ID, useValue: platform }]
    });
    router = TestBed.inject(Router);
  }

  it('[positive] SSR (server platform) bypasses the check and allows activation', async () => {
    setup('server');
    const result = await TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('[positive] no token allows activation (proceed to login page)', async () => {
    setup('browser');
    authState.getAccessToken.mockResolvedValue(null);
    const result = await TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('[negative] a valid existing session redirects to /dashboard', async () => {
    setup('browser');
    authState.getAccessToken.mockResolvedValue('token');
    authState.isSessionValid.mockResolvedValue(true);
    const result = await TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));
    expect(router.serializeUrl(result as any)).toBe('/dashboard');
  });

  it('[positive] an expired session still allows activation (let the user re-login)', async () => {
    setup('browser');
    authState.getAccessToken.mockResolvedValue('token');
    authState.isSessionValid.mockResolvedValue(false);
    const result = await TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));
    expect(result).toBe(true);
  });
});
