import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthStateService, UserProfile } from './auth-state.service';

/** Real cookie + AES roundtrips run through crypto.subtle (native async work) - a single
 *  microtask tick isn't always enough, so give the event loop a real turn before asserting. */
function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 50));
}

function makeJwt(payload: Record<string, unknown>): string {
  const b64url = (obj: unknown) => btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64url({ alg: 'none' })}.${b64url(payload)}.signature`;
}

describe('AuthStateService', () => {
  let service: AuthStateService;

  beforeEach(() => {
    document.cookie.split(';').forEach((c) => {
      const name = c.split('=')[0]?.trim();
      if (name) document.cookie = `${name}=; Max-Age=0; Path=/`;
    });

    TestBed.configureTestingModule({
      providers: [AuthStateService, provideRouter([]), { provide: PLATFORM_ID, useValue: 'browser' }]
    });
    service = TestBed.inject(AuthStateService);
  });

  const profile: UserProfile = { identity: '123', name: 'Budi', role: 'CHECKER' };

  it('[positive] saveSession stores an encrypted, round-trippable access token', async () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    const accessToken = makeJwt({ exp: futureExp });
    const refreshToken = makeJwt({ exp: futureExp });

    await service.saveSession(accessToken, refreshToken, profile, []);
    await flush();

    expect(await service.getAccessToken()).toBe(accessToken);
    expect(await service.getRefreshToken()).toBe(refreshToken);
    expect(await service.getIdentity()).toBe('123');
  });

  it('[negative] saveSession rejects a malformed (non-JWT) access token without storing anything', async () => {
    await service.saveSession('not-a-jwt', makeJwt({ exp: 9999999999 }), profile, []);
    await flush();
    expect(await service.getAccessToken()).toBeNull();
  });

  it('[positive] isSessionValid is true for a token with a future exp', async () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    const token = makeJwt({ exp: futureExp });
    await service.saveSession(token, token, profile, []);
    await flush();

    expect(await service.isSessionValid()).toBe(true);
  });

  it('[negative] isSessionValid is false for an expired token', async () => {
    const pastExp = Math.floor(Date.now() / 1000) - 3600;
    const token = makeJwt({ exp: pastExp });
    await service.saveSession(token, token, profile, []);
    await flush();

    expect(await service.isSessionValid()).toBe(false);
  });

  it('[negative] isSessionValid is false when there is no session at all', async () => {
    expect(await service.isSessionValid()).toBe(false);
  });

  it('[positive] signOut clears cookies and resets observable state to null/empty', async () => {
    const token = makeJwt({ exp: Math.floor(Date.now() / 1000) + 3600 });
    await service.saveSession(token, token, profile, [{ name: 'Dashboard', url: '/dashboard', logo: 'x' }]);
    await flush();

    let latestProfile: UserProfile | null | undefined;
    service.userProfile$.subscribe((p) => (latestProfile = p));
    expect(latestProfile).toEqual(profile);

    await service.signOut();

    expect(await service.getAccessToken()).toBeNull();
    expect(latestProfile).toBeNull();
  });

  it('[negative] getAccessToken returns null on the server platform (SSR) without touching cookies', async () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [AuthStateService, provideRouter([]), { provide: PLATFORM_ID, useValue: 'server' }]
    });
    const serverService = TestBed.inject(AuthStateService);
    expect(await serverService.getAccessToken()).toBeNull();
    expect(await serverService.getIdentity()).toBeNull();
  });
});
