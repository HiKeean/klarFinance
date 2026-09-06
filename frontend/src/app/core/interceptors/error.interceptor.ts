import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { isDevMode } from '@angular/core';
import { catchError, throwError } from 'rxjs';

/** Pesan lebih panjang dari ini dianggap detail teknis (stack trace, exception message), bukan pesan untuk end-user. */
const MAX_USER_MESSAGE_LENGTH = 200;
const GENERIC_ERROR_MESSAGE = 'Terjadi kesalahan pada sistem. Silakan coba lagi nanti.';

/**
 * Menormalkan setiap error HTTP jadi Error dengan message yang siap ditampilkan ke user,
 * supaya komponen tidak lagi menampilkan pesan default Angular seperti "Http failure response for ...".
 */
export const errorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        return throwError(() => new Error(resolveErrorMessage(error)));
      }
      return throwError(() => error);
    })
  );

function resolveErrorMessage(error: HttpErrorResponse): string {
  const backendMessage = extractBackendMessage(error);

  if (backendMessage && backendMessage.length <= MAX_USER_MESSAGE_LENGTH) {
    return backendMessage;
  }

  if (isDevMode()) {
    return backendMessage || error.message || GENERIC_ERROR_MESSAGE;
  }

  console.error('[API] Unexpected error response:', backendMessage || error.message, error);
  return GENERIC_ERROR_MESSAGE;
}

function extractBackendMessage(error: HttpErrorResponse): string {
  const body = error.error;
  const isPlainErrorBody = !!body && typeof body === 'object' && !(body instanceof Error) && !(body instanceof ProgressEvent);
  if (isPlainErrorBody && typeof body.message === 'string') {
    return body.message.trim();
  }
  return '';
}
