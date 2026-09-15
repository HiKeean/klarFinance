import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { LoginUseCase } from './login.use-case';
import { AuthRepository } from '../domain/repositories/auth.repository';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { AuthSession } from '../domain/entities/auth-session';

describe('LoginUseCase', () => {
  let authRepository: { login: ReturnType<typeof vi.fn> };
  let authState: { saveSession: ReturnType<typeof vi.fn> };
  let useCase: LoginUseCase;

  const credentials = { identity: '123', password: 'secret' };

  function session(overrides: Partial<AuthSession> = {}): AuthSession {
    return {
      accessToken: 'a.b.c',
      refreshToken: 'd.e.f',
      userProfile: { identity: '123', name: 'Budi', role: 'CHECKER' },
      menu: [],
      ...overrides
    };
  }

  beforeEach(() => {
    authRepository = { login: vi.fn() };
    authState = { saveSession: vi.fn().mockResolvedValue(undefined) };
    TestBed.configureTestingModule({
      providers: [
        LoginUseCase,
        { provide: AuthRepository, useValue: authRepository },
        { provide: AuthStateService, useValue: authState }
      ]
    });
    useCase = TestBed.inject(LoginUseCase);
  });

  it('[positive] an allowed role saves the session and returns it', async () => {
    authRepository.login.mockReturnValue(of(session()));
    let result: AuthSession | undefined;
    useCase.execute(credentials).subscribe((r) => (result = r));
    await Promise.resolve();
    await Promise.resolve();

    expect(authState.saveSession).toHaveBeenCalledWith('a.b.c', 'd.e.f', session().userProfile, []);
    expect(result).toEqual(session());
  });

  it('[negative] a disallowed role throws before saveSession is called', async () => {
    authRepository.login.mockReturnValue(of(session({ userProfile: { identity: '9', name: 'X', role: 'SUPERADMIN' } })));
    let error: Error | undefined;
    useCase.execute(credentials).subscribe({ error: (e) => (error = e) });
    await Promise.resolve();
    await Promise.resolve();

    expect(error?.message).toBe('Akun ini tidak memiliki akses ke aplikasi Checker & BM.');
    expect(authState.saveSession).not.toHaveBeenCalled();
  });

  it('[negative] a repository error propagates untouched', async () => {
    authRepository.login.mockReturnValue(throwError(() => new Error('Identity atau password salah.')));
    let error: Error | undefined;
    useCase.execute(credentials).subscribe({ error: (e) => (error = e) });
    await Promise.resolve();

    expect(error?.message).toBe('Identity atau password salah.');
    expect(authState.saveSession).not.toHaveBeenCalled();
  });
});
