import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';

export interface MenuItem {
  name: string;
  url: string;
  logo: string;
}

export interface UserProfile {
  identity: string;
  name: string | null;
  role?: string;
}

const AES_KEY_STRING = 'f94d69aaa905ac3d2d0e63fe63b143d3f715e187';
const SESSION_TTL_SECONDS = 2 * 60 * 60;
const IDENTITY_TTL_SECONDS = 30 * 24 * 60 * 60;

function isBrowser(): boolean {
  return typeof document !== 'undefined';
}

function setCookie(name: string, value: string, maxAge: number): void {
  if (!isBrowser()) return;
  const secure = window.location.protocol === 'https:' ? '; Secure' : '';
  document.cookie = `${name}=${encodeURIComponent(value)}; Max-Age=${maxAge}; Path=/; SameSite=Lax${secure}`;
}

function getCookie(name: string): string | null {
  if (!isBrowser()) return null;
  const prefix = `${name}=`;
  const cookie = document.cookie.split('; ').find((item) => item.startsWith(prefix));
  return cookie ? decodeURIComponent(cookie.substring(prefix.length)) : null;
}

function removeCookie(name: string): void {
  setCookie(name, '', 0);
}

async function getAesKey(): Promise<CryptoKey> {
  const keyHash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(AES_KEY_STRING));
  return crypto.subtle.importKey('raw', keyHash, { name: 'AES-GCM' }, false, ['encrypt', 'decrypt']);
}

function toBase64(buffer: ArrayBuffer): string {
  let binary = '';
  for (const byte of new Uint8Array(buffer)) binary += String.fromCharCode(byte);
  return btoa(binary);
}

function fromBase64(value: string): ArrayBuffer {
  const binary = atob(value);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

async function encryptAes(text: string): Promise<string> {
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    await getAesKey(),
    new TextEncoder().encode(text)
  );
  return `${toBase64(iv.buffer)}.${toBase64(encrypted)}`;
}

async function decryptAes(value: string): Promise<string> {
  const parts = value.split('.');
  if (parts.length !== 2) throw new Error('Format cookie tidak valid');
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: new Uint8Array(fromBase64(parts[0])) },
    await getAesKey(),
    fromBase64(parts[1])
  );
  return new TextDecoder().decode(decrypted);
}

function decodeJwtPayload(token: string): Record<string, unknown> {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('Format JWT tidak valid');
  const encoded = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  const padded = encoded.padEnd(encoded.length + ((4 - (encoded.length % 4)) % 4), '=');
  return JSON.parse(atob(padded));
}

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly userProfileSubject = new BehaviorSubject<UserProfile | null>(null);
  private readonly menuListSubject = new BehaviorSubject<MenuItem[]>([]);
  public readonly userProfile$: Observable<UserProfile | null> = this.userProfileSubject.asObservable();
  public readonly menuList$: Observable<MenuItem[]> = this.menuListSubject.asObservable();
  private sessionExpiredShown = false;
  private expiryTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    if (isPlatformBrowser(this.platformId)) void this.loadInitialState();
  }

  async saveSession(accessToken: string, refreshToken: string, userProfile: UserProfile, menuList: MenuItem[]): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      if (accessToken.split('.').length !== 3 || refreshToken.split('.').length !== 3) {
        throw new Error('Format JWT tidak valid.');
      }

      setCookie('auth_access_token', await encryptAes(accessToken), SESSION_TTL_SECONDS);
      setCookie('auth_refresh_token', await encryptAes(refreshToken), SESSION_TTL_SECONDS);
      setCookie('auth_user_profile', JSON.stringify(userProfile), SESSION_TTL_SECONDS);
      setCookie('auth_menu_list', JSON.stringify(menuList), SESSION_TTL_SECONDS);
      setCookie('auth_identity', userProfile?.identity || '', IDENTITY_TTL_SECONDS);

      this.userProfileSubject.next(userProfile);
      this.menuListSubject.next(menuList);
      this.sessionExpiredShown = false;
      this.scheduleSessionExpiry(accessToken);
    } catch (error) {
      console.error('Gagal menyimpan sesi di cookie:', error);
    }
  }

  async getAccessToken(): Promise<string | null> {
    if (!isPlatformBrowser(this.platformId)) return null;
    try {
      const encrypted = getCookie('auth_access_token');
      if (!encrypted) return null;
      return await decryptAes(encrypted);
    } catch {
      return null;
    }
  }

  async getRefreshToken(): Promise<string | null> {
    if (!isPlatformBrowser(this.platformId)) return null;
    try {
      const encrypted = getCookie('auth_refresh_token');
      if (!encrypted) return null;
      return await decryptAes(encrypted);
    } catch {
      return null;
    }
  }

  async getIdentity(): Promise<string | null> {
    return isPlatformBrowser(this.platformId) ? getCookie('auth_identity') : null;
  }

  async isSessionValid(): Promise<boolean> {
    const token = await this.getAccessToken();
    if (!token) return false;
    try {
      const payload = decodeJwtPayload(token);
      return typeof payload['exp'] === 'number' && (payload['exp'] as number) * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  async notifySessionExpired(): Promise<void> {
    if (!isPlatformBrowser(this.platformId) || this.sessionExpiredShown) return;
    this.sessionExpiredShown = true;
    window.alert('Token expired. Silakan masukkan password untuk melanjutkan.');
  }

  private scheduleSessionExpiry(token: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    if (this.expiryTimer) clearTimeout(this.expiryTimer);
    try {
      const payload = decodeJwtPayload(token);
      if (typeof payload['exp'] !== 'number') return;
      this.expiryTimer = setTimeout(async () => {
        await this.notifySessionExpired();
        await this.router.navigateByUrl('/reauth');
      }, Math.max(0, (payload['exp'] as number) * 1000 - Date.now()));
    } catch {
      /* Guard/interceptor tetap menangani token invalid. */
    }
  }

  async signOut(): Promise<void> {
    if (this.expiryTimer) clearTimeout(this.expiryTimer);
    if (isBrowser()) {
      ['auth_access_token', 'auth_refresh_token', 'auth_user_profile', 'auth_menu_list', 'auth_identity']
        .forEach(removeCookie);
    }
    this.userProfileSubject.next(null);
    this.menuListSubject.next([]);
  }

  private async loadInitialState(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      const profile = getCookie('auth_user_profile');
      const menu = getCookie('auth_menu_list');
      if (profile) this.userProfileSubject.next(JSON.parse(profile));
      if (menu) this.menuListSubject.next(JSON.parse(menu));
    } catch (error) {
      console.error('Gagal load state awal dari cookie:', error);
    }
  }
}
