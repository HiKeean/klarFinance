import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MainLayoutComponent } from './main-layout';
import { AuthStateService } from '../../services/auth-state.service';

describe('MainLayoutComponent', () => {
  it('[positive] creates the layout shell', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStateService, useValue: { userProfile$: of(null) } }]
    });
    const fixture = TestBed.createComponent(MainLayoutComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });
});
