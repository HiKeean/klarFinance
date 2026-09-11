import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { NplReportServices } from './npl-report-services';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';

describe('NplReportServices', () => {
  let service: NplReportServices;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'delete']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    service = TestBed.inject(NplReportServices);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('positive', () => {
    it('getReport calls the NPL report endpoint with no params', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getReport().subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.dbo.nplReport);
    });

    it('resolves the report items on success', (done) => {
      const items = [{ branchId: 1 }];
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: items }));

      service.getReport().subscribe((res) => {
        expect(res.data).toEqual(items as any);
        done();
      });
    });
  });

  describe('negative', () => {
    it('propagates a transport error', (done) => {
      api.get.and.returnValue(throwError(() => new Error('timeout')));

      service.getReport().subscribe({
        next: () => fail('expected an error'),
        error: (err: Error) => {
          expect(err.message).toBe('timeout');
          done();
        }
      });
    });
  });
});
