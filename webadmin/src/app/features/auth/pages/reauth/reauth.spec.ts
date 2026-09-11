import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ReauthPage } from './reauth';
import { AuthApiService } from '../../services/auth-api.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';

function setValue(fixture: ComponentFixture<ReauthPage>, selector: string, value: string): void {
  const input: HTMLInputElement = fixture.nativeElement.querySelector(selector);
  input.value = value;
  input.dispatchEvent(new Event('input'));
  fixture.detectChanges();
}

describe('ReauthPage', () => {
  let fixture: ComponentFixture<ReauthPage>;
  let component: ReauthPage;
  let authApi: jasmine.SpyObj<AuthApiService>;
  let authState: jasmine.SpyObj<AuthStateService>;
  let router: Router;

  beforeEach(async () => {
    authApi = jasmine.createSpyObj<AuthApiService>('AuthApiService', ['login']);
    authState = jasmine.createSpyObj<AuthStateService>('AuthStateService', ['getIdentity', 'signOut']);
    authState.getIdentity.and.resolveTo('20260101');
    authState.signOut.and.resolveTo(undefined);

    await TestBed.configureTestingModule({
      imports: [ReauthPage],
      providers: [
        provideRouter([]),
        { provide: AuthApiService, useValue: authApi },
        { provide: AuthStateService, useValue: authState }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReauthPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('[positive] prefills the identity field from the stored session identity', () => {
    const identityInput: HTMLInputElement = fixture.nativeElement.querySelector('#identity');
    expect(identityInput.value).toBe('20260101');
    expect(identityInput.readOnly).toBeTrue();
  });

  it('[positive] a successful re-login navigates to /dashboard', async () => {
    authApi.login.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: { accessToken: 'a.b.c', refreshToken: 'r.e.f' } }));

    setValue(fixture, '#password', 'secret1');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(authApi.login).toHaveBeenCalledWith({ identity: '20260101', password: 'secret1' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('[negative] a wrong password shows an error message and does not navigate', async () => {
    authApi.login.and.returnValue(of({ success: false, statusCode: 401, message: 'Password salah.', data: null as any }));

    setValue(fixture, '#password', 'wrong');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    const error: HTMLElement = fixture.nativeElement.querySelector('.error-message');
    expect(error.textContent).toContain('Password salah.');
  });

  it('[negative] a network error surfaces a fallback error message', async () => {
    authApi.login.and.returnValue(throwError(() => ({})));

    setValue(fixture, '#password', 'secret1');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();

    const error: HTMLElement = fixture.nativeElement.querySelector('.error-message');
    expect(error.textContent).toContain('Password salah. Coba lagi.');
  });

  it('[positive] "Change account" signs out and navigates to /login', async () => {
    fixture.nativeElement.querySelector('.secondary-button').click();
    await fixture.whenStable();

    expect(authState.signOut).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
