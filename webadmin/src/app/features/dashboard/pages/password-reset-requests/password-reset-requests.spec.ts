import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PasswordResetRequestsPage } from './password-reset-requests';
import { PasswordResetRequestServices } from '../../services/password-reset-request-services';
import { PasswordResetRequestItem } from '../../models/password-reset-request.model';

const items: PasswordResetRequestItem[] = [
  {
    id: 1,
    identity: '2026001',
    name: 'Budi',
    role: 'BM',
    requestedAt: '2026-01-01T00:00:00Z',
    status: 'PENDING',
    decidedAt: null,
    decidedBy: null,
    reason: null
  }
];

describe('PasswordResetRequestsPage', () => {
  let fixture: ComponentFixture<PasswordResetRequestsPage>;
  let component: PasswordResetRequestsPage;
  let services: jasmine.SpyObj<PasswordResetRequestServices>;

  beforeEach(async () => {
    services = jasmine.createSpyObj<PasswordResetRequestServices>('PasswordResetRequestServices', ['getAll', 'decide']);
    services.getAll.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: items }));

    await TestBed.configureTestingModule({
      imports: [PasswordResetRequestsPage],
      providers: [{ provide: PasswordResetRequestServices, useValue: services }]
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordResetRequestsPage);
    component = fixture.componentInstance;
  });

  it('[positive] loads pending requests on init', () => {
    fixture.detectChanges();

    expect(services.getAll).toHaveBeenCalledWith('PENDING');
    expect(component.requests()).toEqual(items);
    expect(component.isLoading()).toBeFalse();
  });

  it('[negative] clears the list and shows the backend message on success:false', () => {
    services.getAll.and.returnValue(of({ success: false, statusCode: 500, message: 'Gagal ambil data', data: null as any }));

    fixture.detectChanges();

    expect(component.requests()).toEqual([]);
    expect(component.errorMessage()).toBe('Gagal ambil data');
  });

  it('[negative] shows a fallback message on a transport error', () => {
    services.getAll.and.returnValue(throwError(() => new Error('boom')));

    fixture.detectChanges();

    expect(component.requests()).toEqual([]);
    expect(component.errorMessage()).toBe('Gagal memuat permintaan reset password.');
  });

  describe('changeFilter', () => {
    it('[positive] updates the status filter and reloads', () => {
      fixture.detectChanges();
      services.getAll.calls.reset();

      component.changeFilter('APPROVED');

      expect(component.statusFilter()).toBe('APPROVED');
      expect(services.getAll).toHaveBeenCalledWith('APPROVED');
    });
  });

  describe('approve', () => {
    it('[positive] approves after confirmation and reloads', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(true);
      services.decide.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));
      services.getAll.calls.reset();

      component.approve(items[0]);

      expect(services.decide).toHaveBeenCalledWith(1, 'APPROVE');
      expect(component.decidingId()).toBeNull();
      expect(services.getAll).toHaveBeenCalled();
    });

    it('[negative] does not call the API when the user cancels the confirmation', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(false);

      component.approve(items[0]);

      expect(services.decide).not.toHaveBeenCalled();
    });

    it('[negative] alerts the backend message and clears decidingId on success:false', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(true);
      const alertSpy = spyOn(window, 'alert');
      services.decide.and.returnValue(of({ success: false, statusCode: 409, message: 'Sudah diputuskan', data: null }));

      component.approve(items[0]);

      expect(alertSpy).toHaveBeenCalledWith('Sudah diputuskan');
      expect(component.decidingId()).toBeNull();
    });

    it('[negative] alerts a transport error message and clears decidingId', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(true);
      const alertSpy = spyOn(window, 'alert');
      services.decide.and.returnValue(throwError(() => ({ error: { message: 'Server down' } })));

      component.approve(items[0]);

      expect(alertSpy).toHaveBeenCalledWith('Server down');
      expect(component.decidingId()).toBeNull();
    });
  });

  describe('reject', () => {
    it('[positive] rejects with a trimmed reason after a non-blank prompt', () => {
      fixture.detectChanges();
      spyOn(window, 'prompt').and.returnValue('  Data tidak valid  ');
      services.decide.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      component.reject(items[0]);

      expect(services.decide).toHaveBeenCalledWith(1, 'REJECT', 'Data tidak valid');
    });

    it('[negative] does not call the API when the prompt is cancelled (null)', () => {
      fixture.detectChanges();
      spyOn(window, 'prompt').and.returnValue(null);

      component.reject(items[0]);

      expect(services.decide).not.toHaveBeenCalled();
    });

    it('[negative] does not call the API when the reason is blank', () => {
      fixture.detectChanges();
      spyOn(window, 'prompt').and.returnValue('   ');

      component.reject(items[0]);

      expect(services.decide).not.toHaveBeenCalled();
    });
  });
});
