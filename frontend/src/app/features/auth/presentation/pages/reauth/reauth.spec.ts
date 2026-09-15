import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ReauthPage } from './reauth';
import { LoginUseCase } from '../../../application/login.use-case';
import { AuthStateService } from '../../../../../core/services/auth-state.service';

describe('ReauthPage', () => {
  let loginUseCase: { execute: ReturnType<typeof vi.fn> };
  let authState: { getIdentity: ReturnType<typeof vi.fn>; signOut: ReturnType<typeof vi.fn> };
  let router: Router;

  function setup() {
    TestBed.configureTestingModule({
      providers: [
        ReauthPage,
        provideRouter([]),
        { provide: LoginUseCase, useValue: loginUseCase },
        { provide: AuthStateService, useValue: authState }
      ]
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    return TestBed.inject(ReauthPage);
  }

  beforeEach(() => {
    loginUseCase = { execute: vi.fn() };
    authState = { getIdentity: vi.fn().mockResolvedValue('user1'), signOut: vi.fn().mockResolvedValue(undefined) };
  });

  it('[positive] loads the current identity on construction', async () => {
    const page = setup();
    await Promise.resolve();
    await Promise.resolve();
    expect((page as any).identity).toBe('user1');
  });

  it('[negative] falls back to an empty identity when none is stored', async () => {
    authState.getIdentity.mockResolvedValue(null);
    const page = setup();
    await Promise.resolve();
    await Promise.resolve();
    expect((page as any).identity).toBe('');
  });

  it('[positive] submit success navigates to /dashboard', async () => {
    const page = setup();
    await Promise.resolve();
    await Promise.resolve();
    loginUseCase.execute.mockReturnValue(of({} as any));
    (page as any).password = 'secret';
    await (page as any).submit();
    expect(loginUseCase.execute).toHaveBeenCalledWith({ identity: 'user1', password: 'secret' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] submit failure sets errorMessage', async () => {
    const page = setup();
    loginUseCase.execute.mockReturnValue(throwError(() => new Error('Password salah khusus.')));
    await (page as any).submit();
    expect((page as any).errorMessage()).toBe('Password salah khusus.');
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] submit failure without a message falls back to a generic message', async () => {
    const page = setup();
    loginUseCase.execute.mockReturnValue(throwError(() => ({})));
    await (page as any).submit();
    expect((page as any).errorMessage()).toBe('Password salah. Coba lagi.');
  });

  it('[positive] changeAccount signs out and navigates to /login', async () => {
    const page = setup();
    await (page as any).changeAccount();
    expect(authState.signOut).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
