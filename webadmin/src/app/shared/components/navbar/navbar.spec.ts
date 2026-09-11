import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { NavbarComponent } from './navbar';
import { AuthStateService, MenuItem } from '../../../core/services/auth-state.service';

describe('NavbarComponent', () => {
  let fixture: ComponentFixture<NavbarComponent>;
  let authState: jasmine.SpyObj<AuthStateService> & { menuList$: BehaviorSubject<MenuItem[]> };
  let router: Router;

  beforeEach(async () => {
    const menuList$ = new BehaviorSubject<MenuItem[]>([]);
    authState = Object.assign(jasmine.createSpyObj<AuthStateService>('AuthStateService', ['signOut']), { menuList$ }) as any;
    authState.signOut.and.resolveTo(undefined);

    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [provideRouter([]), { provide: AuthStateService, useValue: authState }]
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('[positive] renders one link per RBAC menu item from AuthStateService.menuList$', () => {
    authState.menuList$.next([
      { name: 'User', url: '/dashboard/user', logo: 'person' },
      { name: 'Role', url: '/dashboard/role', logo: 'key' }
    ]);
    fixture.detectChanges();

    const links: NodeListOf<HTMLAnchorElement> = fixture.nativeElement.querySelectorAll('nav a');
    // Dashboard link is hardcoded plus the two RBAC-driven ones.
    expect(links.length).toBe(3);
    expect(Array.from(links).some((a) => a.textContent?.includes('User'))).toBeTrue();
    expect(Array.from(links).some((a) => a.textContent?.includes('Role'))).toBeTrue();
  });

  it('[negative] renders only the hardcoded Dashboard link when the RBAC menu list is empty', () => {
    authState.menuList$.next([]);
    fixture.detectChanges();

    const links: NodeListOf<HTMLAnchorElement> = fixture.nativeElement.querySelectorAll('nav a');
    expect(links.length).toBe(1);
    expect(links[0].textContent).toContain('Dashboard');
  });

  it('[positive] clicking Keluar signs out and navigates to /login', async () => {
    fixture.nativeElement.querySelector('button.logout').click();
    await fixture.whenStable();

    expect(authState.signOut).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
