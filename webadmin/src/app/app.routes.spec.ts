import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { BehaviorSubject } from 'rxjs';
import { routes } from './app.routes';
import { AuthStateService, MenuItem } from './core/services/auth-state.service';

describe('app routing + guards (integration)', () => {
  let authState: jasmine.SpyObj<AuthStateService> & { userProfile$: BehaviorSubject<any>; menuList$: BehaviorSubject<MenuItem[]> };
  let router: Router;

  beforeEach(async () => {
    // Redirect targets render real pages (DashboardPage behind AdminLayoutComponent's
    // navbar/header, or ReauthPage) which also read AuthStateService - stub everything
    // those pages touch, not just the guard-relevant methods.
    authState = Object.assign(
      jasmine.createSpyObj<AuthStateService>('AuthStateService', [
        'getAccessToken',
        'isSessionValid',
        'notifySessionExpired',
        'getIdentity',
        'signOut'
      ]),
      { userProfile$: new BehaviorSubject<any>(null), menuList$: new BehaviorSubject<MenuItem[]>([]) }
    ) as any;
    authState.getIdentity.and.resolveTo('20260101');
    authState.signOut.and.resolveTo(undefined);

    await TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthStateService, useValue: authState }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('[negative] navigating to /dashboard while logged out redirects to /login (authGuard)', async () => {
    authState.getAccessToken.and.resolveTo(null);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/dashboard');

    expect(router.url).toBe('/login');
  });

  it('[positive] navigating to /dashboard with a valid session renders the DashboardPage', async () => {
    authState.getAccessToken.and.resolveTo('a.b.c');
    authState.isSessionValid.and.resolveTo(true);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/dashboard');

    expect(router.url).toBe('/dashboard');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Dashboard');
  });

  it('[negative] navigating to /dashboard with an expired session redirects to /reauth and notifies expiry', async () => {
    authState.getAccessToken.and.resolveTo('a.b.c');
    authState.isSessionValid.and.resolveTo(false);
    authState.notifySessionExpired.and.resolveTo(undefined);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/dashboard');

    expect(router.url).toBe('/reauth');
    expect(authState.notifySessionExpired).toHaveBeenCalled();
  });

  it('[positive] navigating to /login while logged out is allowed (loginGuard)', async () => {
    authState.getAccessToken.and.resolveTo(null);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/login');

    expect(router.url).toBe('/login');
  });

  it('[negative] navigating to /login with an already-valid session redirects to /dashboard (loginGuard)', async () => {
    authState.getAccessToken.and.resolveTo('a.b.c');
    authState.isSessionValid.and.resolveTo(true);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/login');

    expect(router.url).toBe('/dashboard');
  });

  it('[positive] an unknown URL falls back to /login (wildcard route)', async () => {
    authState.getAccessToken.and.resolveTo(null);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/this-page-does-not-exist');

    expect(router.url).toBe('/login');
  });
});
