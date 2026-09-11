import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { HeaderComponent } from './header';
import { AuthStateService, MenuItem } from '../../../core/services/auth-state.service';

describe('HeaderComponent', () => {
  let fixture: ComponentFixture<HeaderComponent>;
  let authState: { userProfile$: BehaviorSubject<any>; menuList$: BehaviorSubject<MenuItem[]> };

  beforeEach(async () => {
    authState = { userProfile$: new BehaviorSubject<any>(null), menuList$: new BehaviorSubject<MenuItem[]>([]) };

    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [provideRouter([]), { provide: AuthStateService, useValue: authState }]
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('[negative] falls back to "Dashboard" as the page title when no RBAC menu item matches the current URL', () => {
    fixture.detectChanges();
    const title: HTMLElement = fixture.nativeElement.querySelector('.page-title');
    expect(title.textContent?.trim()).toBe('Dashboard');
  });

  it('[positive] shows the logged-in user\'s name once userProfile$ emits', () => {
    authState.userProfile$.next({ identity: 'superadmin', name: 'Super Admin' });
    fixture.detectChanges();

    const name: HTMLElement = fixture.nativeElement.querySelector('.profile-name');
    expect(name.textContent).toContain('Super Admin');
  });

  it('[negative] falls back to the identity when the profile has no name', () => {
    authState.userProfile$.next({ identity: 'superadmin', name: null });
    fixture.detectChanges();

    const name: HTMLElement = fixture.nativeElement.querySelector('.profile-name');
    expect(name.textContent).toContain('superadmin');
  });

  it('[negative] shows no profile name element while userProfile$ is null (not yet logged in)', () => {
    const name: HTMLElement | null = fixture.nativeElement.querySelector('.profile-name');
    expect(name).toBeNull();
  });
});
