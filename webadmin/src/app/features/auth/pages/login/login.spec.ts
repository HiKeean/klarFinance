import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginPage } from './login';
import { AuthApiService } from '../../services/auth-api.service';

function setValue(fixture: ComponentFixture<LoginPage>, selector: string, value: string): void {
  const input: HTMLInputElement = fixture.nativeElement.querySelector(selector);
  input.value = value;
  input.dispatchEvent(new Event('input'));
  fixture.detectChanges();
}

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let component: LoginPage;
  let authApi: jasmine.SpyObj<AuthApiService>;
  let router: Router;

  beforeEach(async () => {
    authApi = jasmine.createSpyObj<AuthApiService>('AuthApiService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideRouter([]), { provide: AuthApiService, useValue: authApi }]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('[negative] the submit button starts disabled because the required fields are empty', async () => {
    await fixture.whenStable();
    fixture.detectChanges();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(button.disabled).toBeTrue();
  });

  it('[positive] filling both fields enables the submit button', async () => {
    setValue(fixture, '#identity', '20260101');
    setValue(fixture, '#password', 'secret1');
    await fixture.whenStable();
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(button.disabled).toBeFalse();
  });

  it('[positive] a successful login navigates to /dashboard', async () => {
    authApi.login.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: { accessToken: 'a.b.c', refreshToken: 'r.e.f' } }));

    setValue(fixture, '#identity', '20260101');
    setValue(fixture, '#password', 'secret1');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(authApi.login).toHaveBeenCalledWith({ identity: '20260101', password: 'secret1' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('[negative] a failed login (wrong password) shows an error message and does not navigate', async () => {
    authApi.login.and.returnValue(of({ success: false, statusCode: 401, message: 'Identity atau password salah.', data: null as any }));

    setValue(fixture, '#identity', '20260101');
    setValue(fixture, '#password', 'wrong');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    const error: HTMLElement = fixture.nativeElement.querySelector('.error-message');
    expect(error.textContent).toContain('Identity atau password salah.');
  });

  it('[negative] a network error also shows an error message instead of throwing unhandled', async () => {
    authApi.login.and.returnValue(throwError(() => new Error('Network Error')));

    setValue(fixture, '#identity', '20260101');
    setValue(fixture, '#password', 'secret1');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    const error: HTMLElement = fixture.nativeElement.querySelector('.error-message');
    expect(error.textContent).toContain('Network Error');
  });
});
