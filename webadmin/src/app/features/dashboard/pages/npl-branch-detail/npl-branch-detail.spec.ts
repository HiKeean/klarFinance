import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { NplBranchDetailPage } from './npl-branch-detail';
import { NplReportServices } from '../../services/npl-report-services';
import { BranchLoanDetailItem, BranchLoanPageResponse } from '../../models/npl-report.model';

const sampleLoan: BranchLoanDetailItem = {
  loanId: 1,
  nasabahName: 'Budi',
  loanAmount: 5000000,
  status: 'Overdue',
  daysOverdue: 12
};

const sampleResponse: BranchLoanPageResponse = {
  branchId: 7,
  branchName: 'Cabang Bandung',
  totalAssets: 100000000,
  activeBorrowers: 42,
  loans: { content: [sampleLoan], totalElements: 1, totalPages: 1, currentPage: 0, pageSize: 10 }
};

describe('NplBranchDetailPage', () => {
  let fixture: ComponentFixture<NplBranchDetailPage>;
  let component: NplBranchDetailPage;
  let nplReportServices: jasmine.SpyObj<NplReportServices>;

  function setup(branchId = '7'): void {
    nplReportServices = jasmine.createSpyObj<NplReportServices>('NplReportServices', ['getReport', 'getBranchLoans', 'startCall']);
    nplReportServices.getBranchLoans.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: sampleResponse }));

    TestBed.configureTestingModule({
      imports: [NplBranchDetailPage],
      providers: [
        { provide: NplReportServices, useValue: nplReportServices },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map([['branchId', branchId]]) } }
        }
      ]
    });

    fixture = TestBed.createComponent(NplBranchDetailPage);
    component = fixture.componentInstance;
  }

  beforeEach(() => setup());

  it('[positive] loads page 0 with an empty search on init', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(nplReportServices.getBranchLoans).toHaveBeenCalledWith(7, 0, 10, '');
    expect(component.branchName()).toBe('Cabang Bandung');
    expect(component.totalAssets()).toBe(100000000);
    expect(component.activeBorrowers()).toBe(42);
    expect(component.loans()).toEqual([sampleLoan]);
    expect(component.totalElements()).toBe(1);
    expect(component.totalPages()).toBe(1);
    expect(component.isLoading()).toBeFalse();
  });

  describe('negative: loading', () => {
    it('[negative] surfaces the backend message and stops loading when success is false', () => {
      nplReportServices.getBranchLoans.and.returnValue(
        of({ success: false, statusCode: 404, message: 'Branch not found', data: null as any })
      );

      fixture.detectChanges();

      expect(component.errorMessage()).toBe('Branch not found');
      expect(component.isLoading()).toBeFalse();
    });

    it('[negative] sets a generic error message and stops loading on a transport error', () => {
      nplReportServices.getBranchLoans.and.returnValue(throwError(() => new Error('network down')));

      fixture.detectChanges();

      expect(component.errorMessage()).toBe('Gagal memuat data pinjaman branch.');
      expect(component.isLoading()).toBeFalse();
    });
  });

  describe('search', () => {
    // Zoneless app - debounceTime(400) is a real RxJS timer here, exercised with a real
    // (short) wait instead of a virtual clock, matching the pattern in user.spec.ts.
    it('[positive] debounces search input, resets to page 0, and reloads with the trimmed term', async () => {
      fixture.detectChanges();
      nplReportServices.getBranchLoans.calls.reset();
      component.pageIndex.set(2);

      component.onSearch('  budi  ');
      await new Promise((resolve) => setTimeout(resolve, 200));
      expect(nplReportServices.getBranchLoans).not.toHaveBeenCalled();

      await new Promise((resolve) => setTimeout(resolve, 300));
      expect(component.pageIndex()).toBe(0);
      expect(nplReportServices.getBranchLoans).toHaveBeenCalledWith(7, 0, 10, 'budi');
    });
  });

  describe('pagination', () => {
    it('[negative] previousPage is a no-op on the first page', () => {
      fixture.detectChanges();
      nplReportServices.getBranchLoans.calls.reset();

      component.previousPage();

      expect(nplReportServices.getBranchLoans).not.toHaveBeenCalled();
    });

    it('[positive] previousPage reloads the prior page when not on the first page', () => {
      fixture.detectChanges();
      component.pageIndex.set(1);
      nplReportServices.getBranchLoans.calls.reset();

      component.previousPage();

      expect(component.pageIndex()).toBe(0);
      expect(nplReportServices.getBranchLoans).toHaveBeenCalledWith(7, 0, 10, '');
    });

    it('[negative] nextPage is a no-op on the last page', () => {
      fixture.detectChanges();
      nplReportServices.getBranchLoans.calls.reset();

      component.nextPage();

      expect(nplReportServices.getBranchLoans).not.toHaveBeenCalled();
    });

    it('[positive] nextPage reloads the next page when more pages exist', () => {
      nplReportServices.getBranchLoans.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: { ...sampleResponse, loans: { ...sampleResponse.loans, totalPages: 2 } }
      }));
      fixture.detectChanges();
      nplReportServices.getBranchLoans.calls.reset();

      component.nextPage();

      expect(component.pageIndex()).toBe(1);
      expect(nplReportServices.getBranchLoans).toHaveBeenCalledWith(7, 1, 10, '');
    });

    describe('goToPage', () => {
      it('[negative] does nothing when the target page equals the current page', () => {
        fixture.detectChanges();
        nplReportServices.getBranchLoans.calls.reset();

        component.goToPage(0);

        expect(nplReportServices.getBranchLoans).not.toHaveBeenCalled();
      });

      it('[negative] does nothing for a negative page index', () => {
        fixture.detectChanges();
        nplReportServices.getBranchLoans.calls.reset();

        component.goToPage(-1);

        expect(nplReportServices.getBranchLoans).not.toHaveBeenCalled();
      });

      it('[negative] does nothing when the target page is beyond totalPages', () => {
        fixture.detectChanges();
        nplReportServices.getBranchLoans.calls.reset();

        component.goToPage(5);

        expect(nplReportServices.getBranchLoans).not.toHaveBeenCalled();
      });

      it('[positive] reloads when a valid different page is requested', () => {
        nplReportServices.getBranchLoans.and.returnValue(of({
          success: true, statusCode: 200, message: 'OK',
          data: { ...sampleResponse, loans: { ...sampleResponse.loans, totalPages: 3 } }
        }));
        fixture.detectChanges();
        nplReportServices.getBranchLoans.calls.reset();

        component.goToPage(2);

        expect(component.pageIndex()).toBe(2);
        expect(nplReportServices.getBranchLoans).toHaveBeenCalledWith(7, 2, 10, '');
      });
    });

    describe('visiblePages', () => {
      it('[positive] returns every page when totalPages <= 5', () => {
        fixture.detectChanges();
        component.totalPages.set(3);

        expect(component.visiblePages()).toEqual([0, 1, 2]);
      });

      it('[positive] anchors the window to the start when current page is near the beginning', () => {
        fixture.detectChanges();
        component.totalPages.set(10);
        component.pageIndex.set(1);

        expect(component.visiblePages()).toEqual([0, 1, 2, 3, 4]);
      });

      it('[positive] anchors the window to the end when current page is near the end', () => {
        fixture.detectChanges();
        component.totalPages.set(10);
        component.pageIndex.set(8);

        expect(component.visiblePages()).toEqual([5, 6, 7, 8, 9]);
      });

      it('[positive] centers the window around the current page otherwise', () => {
        fixture.detectChanges();
        component.totalPages.set(10);
        component.pageIndex.set(5);

        expect(component.visiblePages()).toEqual([3, 4, 5, 6, 7]);
      });
    });
  });

  describe('startEntry / endEntry', () => {
    it('[negative] both are 0 when there are no elements', () => {
      nplReportServices.getBranchLoans.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: { ...sampleResponse, loans: { content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 10 } }
      }));
      fixture.detectChanges();

      expect(component.startEntry).toBe(0);
      expect(component.endEntry).toBe(0);
    });

    it('[positive] computes the 1-based range for the current page', () => {
      nplReportServices.getBranchLoans.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: { ...sampleResponse, loans: { content: [sampleLoan], totalElements: 25, totalPages: 3, currentPage: 1, pageSize: 10 } }
      }));
      fixture.detectChanges();
      component.pageIndex.set(1);

      expect(component.startEntry).toBe(11);
      expect(component.endEntry).toBe(20);
    });

    it('[positive] caps endEntry at totalElements on the last (partial) page', () => {
      nplReportServices.getBranchLoans.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: { ...sampleResponse, loans: { content: [sampleLoan], totalElements: 25, totalPages: 3, currentPage: 2, pageSize: 10 } }
      }));
      fixture.detectChanges();
      component.pageIndex.set(2);

      expect(component.startEntry).toBe(21);
      expect(component.endEntry).toBe(25);
    });
  });

  describe('formatting helpers', () => {
    it('[positive] formatAmount formats as Indonesian Rupiah', () => {
      fixture.detectChanges();
      expect(component.formatAmount(5000000)).toBe('Rp 5.000.000');
    });

    it('[positive] statusLabel maps Current to "Sehat" and passes other statuses through', () => {
      fixture.detectChanges();
      expect(component.statusLabel('Current')).toBe('Sehat');
      expect(component.statusLabel('Overdue')).toBe('Overdue');
      expect(component.statusLabel('Lunas')).toBe('Lunas');
    });

    it('[positive] statusClass maps each known status and falls back to badge-current', () => {
      fixture.detectChanges();
      expect(component.statusClass('Overdue')).toBe('badge-overdue');
      expect(component.statusClass('Lunas')).toBe('badge-lunas');
      expect(component.statusClass('Current')).toBe('badge-current');
    });
  });

  describe('call button (demo deskcall)', () => {
    beforeEach(() => fixture.detectChanges());

    it('[positive] starts a call for the loan after confirmation and reports ringing', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      const alertSpy = spyOn(window, 'alert');
      nplReportServices.startCall.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: { callId: 'c-1', loanId: 1, customerName: 'Budi', ringTimeoutSeconds: 45, pushSent: true }
      }));

      component.call(sampleLoan);

      expect(nplReportServices.startCall).toHaveBeenCalledWith(1);
      expect(alertSpy).toHaveBeenCalledWith(jasmine.stringContaining('sedang berdering'));
      expect(component.callingLoanId()).toBeNull();
    });

    it('[negative] does nothing when the confirmation is cancelled', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      component.call(sampleLoan);

      expect(nplReportServices.startCall).not.toHaveBeenCalled();
    });

    it('[negative] shows the backend message when the call is rejected', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      const alertSpy = spyOn(window, 'alert');
      nplReportServices.startCall.and.returnValue(throwError(() => new Error('FCM token kosong')));

      component.call(sampleLoan);

      expect(alertSpy).toHaveBeenCalledWith('FCM token kosong');
      expect(component.callingLoanId()).toBeNull();
    });
  });
});
