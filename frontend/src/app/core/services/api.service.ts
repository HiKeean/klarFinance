import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { from, Observable, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response';

type QueryParams = HttpParams | Record<string, string | number | boolean>;

function bodyToString(body: unknown): string {
  if (body === null || body === undefined) return '';
  return typeof body === 'string' ? body : JSON.stringify(body) ?? '';
}

function toBase64(bytes: ArrayBuffer): string {
  let binary = '';
  for (const byte of new Uint8Array(bytes)) binary += String.fromCharCode(byte);
  return btoa(binary);
}

async function createHeaders(method: string, url: string, body: unknown): Promise<HttpHeaders> {
  const timestamp = Date.now().toString();
  const origin = typeof window === 'undefined' ? 'http://localhost' : window.location.origin;
  const uri = new URL(url, origin).pathname;
  const stringToSign = method + uri + timestamp + bodyToString(body) + environment.api.apiKey;
  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(environment.api.secretKey),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  const signature = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(stringToSign));

  return new HttpHeaders({
    'X-Timestamp': timestamp,
    'X-Signature': toBase64(signature),
    'X-Client-Type': 'ANGULAR'
  });
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  get<T>(url: string, params?: QueryParams): Observable<ApiResponse<T>> {
    const requestUrl = this.withParams(url, params);
    return from(createHeaders('GET', requestUrl, '')).pipe(
      switchMap((headers) => this.http.get<ApiResponse<T>>(requestUrl, { headers }))
    );
  }

  /** Buat endpoint yang balikin raw bytes (bukan ApiResponse<T> envelope JSON) - mis. foto
   * KTP/selfie. HMAC+Authorization header tetap sama (lewat createHeaders + authInterceptor),
   * cuma responseType-nya blob supaya bisa langsung dijadiin object URL buat <img src>. */
  getBlob(url: string): Observable<Blob> {
    return from(createHeaders('GET', url, '')).pipe(
      switchMap((headers) => this.http.get(url, { headers, responseType: 'blob' }))
    );
  }

  post<T>(url: string, body?: unknown, params?: QueryParams): Observable<ApiResponse<T>> {
    const requestUrl = this.withParams(url, params);
    return from(createHeaders('POST', requestUrl, body)).pipe(
      switchMap((headers) => this.http.post<ApiResponse<T>>(requestUrl, body, { headers }))
    );
  }

  delete<T>(url: string, params?: QueryParams): Observable<ApiResponse<T>> {
    const requestUrl = this.withParams(url, params);
    return from(createHeaders('DELETE', requestUrl, '')).pipe(
      switchMap((headers) => this.http.delete<ApiResponse<T>>(requestUrl, { headers }))
    );
  }

  put<T>(url: string, body?: unknown): Observable<ApiResponse<T>> {
    return from(createHeaders('PUT', url, body)).pipe(
      switchMap((headers) => this.http.put<ApiResponse<T>>(url, body, { headers }))
    );
  }

  private withParams(url: string, params?: QueryParams): string {
    if (!params) return url;
    const query = params instanceof HttpParams
      ? params.toString()
      : new HttpParams({ fromObject: params }).toString();
    return query ? `${url}${url.includes('?') ? '&' : '?'}${query}` : url;
  }
}
