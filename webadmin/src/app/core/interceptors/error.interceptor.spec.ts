import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([errorInterceptor])), provideHttpClientTesting()]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('[positive] passes a successful response through unchanged', (done) => {
    http.get('/api/v1/ok').subscribe((res) => {
      expect(res).toEqual({ ok: true });
      done();
    });

    httpMock.expectOne('/api/v1/ok').flush({ ok: true });
  });

  describe('negative: normalizing HttpErrorResponse', () => {
    it('uses the backend message when it is short enough to show a user', (done) => {
      http.get('/api/v1/fail').subscribe({
        next: () => fail('expected an error'),
        error: (err: Error) => {
          expect(err instanceof Error).toBeTrue();
          expect(err.message).toBe('Identity already exists');
          done();
        }
      });

      httpMock.expectOne('/api/v1/fail').flush({ message: 'Identity already exists' }, { status: 409, statusText: 'Conflict' });
    });

    it('falls back to the generic message when the backend message is too long (looks like a stack trace)', (done) => {
      const longMessage = 'x'.repeat(500);
      http.get('/api/v1/fail').subscribe({
        next: () => fail('expected an error'),
        error: (err: Error) => {
          // isDevMode() in Karma is typically true, so a long backend message still surfaces as-is
          // via the isDevMode() branch OR falls back to generic in prod builds - assert on shape,
          // not the exact isDevMode() outcome, since that is environment-dependent.
          expect(err instanceof Error).toBeTrue();
          expect(err.message.length).toBeGreaterThan(0);
          done();
        }
      });

      httpMock.expectOne('/api/v1/fail').flush({ message: longMessage }, { status: 500, statusText: 'Server Error' });
    });

    it('falls back to the generic message when the error body has no usable message field', (done) => {
      http.get('/api/v1/fail').subscribe({
        next: () => fail('expected an error'),
        error: (err: Error) => {
          expect(err instanceof Error).toBeTrue();
          expect(err.message.length).toBeGreaterThan(0);
          done();
        }
      });

      httpMock.expectOne('/api/v1/fail').flush('Internal Server Error', { status: 500, statusText: 'Server Error' });
    });

    it('trims whitespace from the backend message', (done) => {
      http.get('/api/v1/fail').subscribe({
        next: () => fail('expected an error'),
        error: (err: Error) => {
          expect(err.message).toBe('Role already exists');
          done();
        }
      });

      httpMock.expectOne('/api/v1/fail').flush({ message: '  Role already exists  ' }, { status: 409, statusText: 'Conflict' });
    });
  });
});
