import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStateService } from './auth-state.service';

/** Base64url-encodes a JWT-shaped (but unsigned) token carrying the given payload - enough for
 *  AuthStateService, which only decodes the payload locally and never verifies the signature. */
function fakeJwt(payload: Record<string, unknown>): string {
  const toB64Url = (obj: unknown) => btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${toB64Url({ alg: 'none', typ: 'JWT' })}.${toB64Url(payload)}.signature`;
}

function clearAuthCookies(): void {
  ['auth_access_token', 'auth_refresh_token', 'auth_user_profile', 'auth_menu_list', 'auth_identity'].forEach((name) => {
    document.cookie = `${name}=; Max-Age=0; Path=/`;
  });
}

describe('AuthStateService', () => {
  let service: AuthStateService;

  beforeEach(() => {
    clearAuthCookies();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    service = TestBed.inject(AuthStateService);
  });

  afterEach(() => clearAuthCookies());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('positive: session lifecycle', () => {
    it('round-trips access/refresh tokens and profile/menu through saveSession', async () => {
      const accessToken = fakeJwt({ sub: 'superadmin', exp: Math.floor(Date.now() / 1000) + 3600 });
      const refreshToken = fakeJwt({ sub: 'superadmin', exp: Math.floor(Date.now() / 1000) + 7200 });
      const profile = { identity: 'superadmin', name: 'Super Admin' };
      const menu = [{ name: 'User', url: '/dashboard/user', logo: 'person' }];

      await service.saveSession(accessToken, refreshToken, profile, menu);

      expect(await service.getAccessToken()).toBe(accessToken);
      expect(await service.getRefreshToken()).toBe(refreshToken);
      expect(await service.getIdentity()).toBe('superadmin');
    });

    it('publishes the saved profile and menu on userProfile$/menuList$', async () => {
      const accessToken = fakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const profile = { identity: 'superadmin', name: 'Super Admin' };
      const menu = [{ name: 'Role', url: '/dashboard/role', logo: 'key' }];

      await service.saveSession(accessToken, 'r.e.f', profile, menu);

      let latestProfile: unknown;
      let latestMenu: unknown;
      service.userProfile$.subscribe((p) => (latestProfile = p));
      service.menuList$.subscribe((m) => (latestMenu = m));

      expect(latestProfile).toEqual(profile);
      expect(latestMenu).toEqual(menu);
    });

    it('isSessionValid resolves true for a token whose exp is in the future', async () => {
      const accessToken = fakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600 });
      await service.saveSession(accessToken, 'r.e.f', { identity: 'x' }, []);

      expect(await service.isSessionValid()).toBeTrue();
    });

    it('signOut clears cookies and resets userProfile$/menuList$ to empty', async () => {
      const accessToken = fakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600 });
      await service.saveSession(accessToken, 'r.e.f', { identity: 'x' }, [{ name: 'A', url: '/a', logo: 'a' }]);

      await service.signOut();

      expect(await service.getAccessToken()).toBeNull();
      let latestProfile: unknown = 'not-set';
      let latestMenu: unknown = 'not-set';
      service.userProfile$.subscribe((p) => (latestProfile = p));
      service.menuList$.subscribe((m) => (latestMenu = m));
      expect(latestProfile).toBeNull();
      expect(latestMenu).toEqual([]);
    });
  });

  describe('negative: missing/invalid session state', () => {
    it('getAccessToken resolves null when no session was ever saved', async () => {
      expect(await service.getAccessToken()).toBeNull();
    });

    it('isSessionValid resolves false when there is no token at all', async () => {
      expect(await service.isSessionValid()).toBeFalse();
    });

    it('isSessionValid resolves false for a token whose exp is in the past', async () => {
      const expiredToken = fakeJwt({ exp: Math.floor(Date.now() / 1000) - 3600 });
      await service.saveSession(expiredToken, fakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600 }), { identity: 'x' }, []);

      expect(await service.isSessionValid()).toBeFalse();
    });

    it('saveSession rejects a malformed (non-JWT-shaped) access token and does not persist it', async () => {
      await service.saveSession('not-a-jwt', fakeJwt({ exp: Math.floor(Date.now() / 1000) + 3600 }), { identity: 'x' }, []);

      expect(await service.getAccessToken()).toBeNull();
    });

    it('getIdentity resolves null when nothing was ever saved', async () => {
      expect(await service.getIdentity()).toBeNull();
    });
  });
});
