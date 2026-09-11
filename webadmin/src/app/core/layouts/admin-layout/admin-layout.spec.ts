import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { AdminLayoutComponent } from './admin-layout';
import { AuthStateService, MenuItem } from '../../services/auth-state.service';

describe('AdminLayoutComponent', () => {
  let fixture: ComponentFixture<AdminLayoutComponent>;

  beforeEach(async () => {
    const authState = { userProfile$: new BehaviorSubject<any>(null), menuList$: new BehaviorSubject<MenuItem[]>([]) };

    await TestBed.configureTestingModule({
      imports: [AdminLayoutComponent],
      providers: [provideRouter([]), { provide: AuthStateService, useValue: authState }]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLayoutComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('[positive] composes the sidebar navbar, top header and a router-outlet for the page content', () => {
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('app-navbar')).toBeTruthy();
    expect(root.querySelector('app-header')).toBeTruthy();
    expect(root.querySelector('.page-content')).toBeTruthy();
  });
});
