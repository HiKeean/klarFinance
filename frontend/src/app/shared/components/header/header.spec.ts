import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { HeaderComponent } from './header';
import { AuthStateService, UserProfile } from '../../../core/services/auth-state.service';

describe('HeaderComponent', () => {
  let fixture: ComponentFixture<HeaderComponent>;
  let authState: { userProfile$: Observable<UserProfile | null>; signOut: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(() => {
    authState = { userProfile$: of({ identity: '1', name: 'Budi', role: 'CHECKER' }), signOut: vi.fn().mockResolvedValue(undefined) };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStateService, useValue: authState }]
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();
  });

  it('[positive] creates and reflects the profile name from userProfile$', () => {
    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Budi');
  });

  it('[positive] logout signs out and navigates to /login', async () => {
    await (fixture.componentInstance as any).logout();
    expect(authState.signOut).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
