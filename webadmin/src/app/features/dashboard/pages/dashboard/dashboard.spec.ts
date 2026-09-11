import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardPage } from './dashboard';

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [DashboardPage] }).compileComponents();
    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('[positive] renders the welcome heading', () => {
    const heading: HTMLElement = fixture.nativeElement.querySelector('h1');
    expect(heading.textContent).toContain('Dashboard');
  });
});
