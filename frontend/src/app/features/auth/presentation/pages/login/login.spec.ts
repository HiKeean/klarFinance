import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginPage } from './login';
import { LoginUseCase } from '../../../application/login.use-case';
import { PasswordResetApiService } from '../../../infrastructure/password-reset-api.service';

describe('LoginPage', () => {
  let loginUseCase: { execute: ReturnType<typeof vi.fn> };
  let passwordResetApi: { submit: ReturnType<typeof vi.fn> };
  let router: Router;
  let page: LoginPage;

  beforeEach(() => {
    loginUseCase = { execute: vi.fn() };
    passwordResetApi = { submit: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        LoginPage,
        provideRouter([]),
        { provide: LoginUseCase, useValue: loginUseCase },
        { provide: PasswordResetApiService, useValue: passwordResetApi }
      ]
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    page = TestBed.inject(LoginPage);
  });

  it('[positive] submit success navigates to /dashboard', async () => {
    loginUseCase.execute.mockReturnValue(of({} as any));
    (page as any).identity = 'user1';
    (page as any).password = 'pass';
    await (page as any).submit();
    expect(loginUseCase.execute).toHaveBeenCalledWith({ identity: 'user1', password: 'pass' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] submit failure sets errorMessage and increments failedAttempts', async () => {
    loginUseCase.execute.mockReturnValue(throwError(() => new Error('Identity atau password salah.')));
    await (page as any).submit();
    expect((page as any).errorMessage()).toBe('Identity atau password salah.');
    expect((page as any).failedAttempts()).toBe(1);
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] submit failure without a message falls back to a generic message', async () => {
    loginUseCase.execute.mockReturnValue(throwError(() => ({})));
    await (page as any).submit();
    expect((page as any).errorMessage()).toBe('Identity atau password salah. Coba lagi.');
  });

  it('[positive] showResetOffer flips true after 3 failed attempts', async () => {
    loginUseCase.execute.mockReturnValue(throwError(() => new Error('salah')));
    await (page as any).submit();
    await (page as any).submit();
    expect((page as any).showResetOffer()).toBe(false);
    await (page as any).submit();
    expect((page as any).showResetOffer()).toBe(true);
  });

  it('[positive] onIdentityChange resets failedAttempts and closes the reset form', async () => {
    loginUseCase.execute.mockReturnValue(throwError(() => new Error('salah')));
    await (page as any).submit();
    (page as any).showResetForm.set(true);

    (page as any).onIdentityChange('newuser');
    expect((page as any).identity).toBe('newuser');
    expect((page as any).failedAttempts()).toBe(0);
    expect((page as any).showResetForm()).toBe(false);
  });

  it('[positive] openResetForm seeds resetIdentity from the current identity and opens the form', () => {
    (page as any).identity = 'user1';
    (page as any).openResetForm();
    expect((page as any).resetIdentity).toBe('user1');
    expect((page as any).showResetForm()).toBe(true);
  });

  it('[positive] closeResetForm closes the form', () => {
    (page as any).showResetForm.set(true);
    (page as any).closeResetForm();
    expect((page as any).showResetForm()).toBe(false);
  });

  it('[positive] submitResetRequest success sets resetSuccessMessage and resets failedAttempts', async () => {
    passwordResetApi.submit.mockReturnValue(of(undefined));
    (page as any).failedAttempts.set(3);
    (page as any).resetIdentity = 'user1';
    await (page as any).submitResetRequest();
    expect(passwordResetApi.submit).toHaveBeenCalledWith('user1');
    expect((page as any).resetSuccessMessage()).toContain('Permintaan reset password terkirim');
    expect((page as any).failedAttempts()).toBe(0);
    expect((page as any).resetSubmitting()).toBe(false);
  });

  it('[negative] submitResetRequest failure sets resetErrorMessage', async () => {
    passwordResetApi.submit.mockReturnValue(throwError(() => new Error('Gagal mengirim permintaan reset password khusus.')));
    await (page as any).submitResetRequest();
    expect((page as any).resetErrorMessage()).toBe('Gagal mengirim permintaan reset password khusus.');
    expect((page as any).resetSubmitting()).toBe(false);
  });
});
