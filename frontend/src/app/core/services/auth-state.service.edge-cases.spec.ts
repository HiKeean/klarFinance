import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthStateService, UserProfile } from './auth-state.service';

/** Kept separate from auth-state.service.spec.ts (which is not to be edited) - covers extra
 *  branches: corrupted cookies, malformed initial-state JSON, the notifySessionExpired latch,
 *  and the session-expiry timer. Same real-event-loop-turn convention as the original spec. */
function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 50));
}

function makeJwt(payload: Record<string, unknown>): string {
  const b64url = (obj: unknown) => btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64url({ alg: 'none' })}.${b64url(payload)}.signature`;
}

function clearCookies(): void {
  document.cookie.split(';').forEach((c) => {
    const name = c.split('=')[0]?.trim();
    if (name) document.cookie = `${name}=; Max-Age=0; Path=/`;
  });
}

describe('AuthStateService edge cases', () => {
  let service: AuthStateService;
  const profile: UserProfile = { identity: '123', name: 'Budi', role: 'CHECKER' };

  beforeEach(() => {
    clearCookies();
    TestBed.configureTestingModule({
      providers: [AuthStateService, provideRouter([{ path: 'reauth', children: [] }]), { provide: PLATFORM_ID, useValue: 'browser' }]
    });
    service = TestBed.inject(AuthStateService);
  });

  afterEach(() => clearCookies());

  it('[positive] getRefreshToken returns the stored, round-trippable refresh token', async () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    const token = makeJwt({ exp: futureExp });
    await service.saveSession(token, token, profile, []);
    await flush();
    expect(await service.getRefreshToken()).toBe(token);
  });

  it('[negative] getRefreshToken returns null when there is no session', async () => {
    expect(await service.getRefreshToken()).toBeNull();
  });

  it('[negative] getAccessToken returns null when the stored cookie is corrupted (not a valid AES payload)', async () => {
    document.cookie = 'auth_access_token=not-a-valid-encrypted-value; Path=/';
    expect(await service.getAccessToken()).toBeNull();
  });

  it('[negative] getRefreshToken returns null when the stored cookie is corrupted', async () => {
    document.cookie = 'auth_refresh_token=also-not-valid; Path=/';
    expect(await service.getRefreshToken()).toBeNull();
  });

  it('[negative] loadInitialState swallows malformed JSON in the profile/menu cookies without throwing', async () => {
    document.cookie = 'auth_user_profile=not-json{{; Path=/';
    document.cookie = 'auth_menu_list=not-json[[; Path=/';
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [AuthStateService, provideRouter([]), { provide: PLATFORM_ID, useValue: 'browser' }]
    });
    const freshService = TestBed.inject(AuthStateService);
    await flush();

    let latestProfile: UserProfile | null | undefined = undefined;
    freshService.userProfile$.subscribe((p) => (latestProfile = p));
    expect(latestProfile).toBeNull();
    consoleSpy.mockRestore();
  });

  it('[positive] notifySessionExpired shows the alert only once even when called twice', async () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    await service.notifySessionExpired();
    await service.notifySessionExpired();
    expect(alertSpy).toHaveBeenCalledTimes(1);
    alertSpy.mockRestore();
  });

  it('[negative] notifySessionExpired does nothing on the server platform', async () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [AuthStateService, provideRouter([]), { provide: PLATFORM_ID, useValue: 'server' }]
    });
    const serverService = TestBed.inject(AuthStateService);
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    await serverService.notifySessionExpired();
    expect(alertSpy).not.toHaveBeenCalled();
    alertSpy.mockRestore();
  });

  it('[positive] scheduleSessionExpiry fires notifySessionExpired and navigates to /reauth once the token expires', async () => {
    vi.useFakeTimers();
    try {
      const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
      const router = TestBed.inject(Router);
      const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

      const soonExp = Math.floor(Date.now() / 1000) + 5;
      const token = makeJwt({ exp: soonExp });
      await service.saveSession(token, token, profile, []);

      await vi.advanceTimersByTimeAsync(6000);

      expect(alertSpy).toHaveBeenCalledTimes(1);
      expect(navigateSpy).toHaveBeenCalledWith('/reauth');
      alertSpy.mockRestore();
    } finally {
      vi.useRealTimers();
    }
  });

  it('[negative] signOut clears the pending expiry timer so it never fires', async () => {
    vi.useFakeTimers();
    try {
      const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
      const soonExp = Math.floor(Date.now() / 1000) + 5;
      const token = makeJwt({ exp: soonExp });
      await service.saveSession(token, token, profile, []);

      await service.signOut();
      await vi.advanceTimersByTimeAsync(6000);

      expect(alertSpy).not.toHaveBeenCalled();
      alertSpy.mockRestore();
    } finally {
      vi.useRealTimers();
    }
  });
});
