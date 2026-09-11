import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Dbo } from './dbo';
import { ApiService } from '../api.service';
import { DBO_URL } from '../../config/url';

describe('Dbo', () => {
  let service: Dbo;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'delete']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    service = TestBed.inject(Dbo);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('positive', () => {
    it('getAllProvinces calls the province endpoint with no search param by default', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAllProvinces().subscribe();

      expect(api.get).toHaveBeenCalledWith(DBO_URL.location.province, {});
    });

    it('getAllRegencies always includes provinceId, plus search when given', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAllRegencies(31, 'jakarta').subscribe();

      expect(api.get).toHaveBeenCalledWith(DBO_URL.location.regency, { provinceId: 31, search: 'jakarta' });
    });

    it('getAllDistricts always includes regenciesId', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAllDistricts(200).subscribe();

      expect(api.get).toHaveBeenCalledWith(DBO_URL.location.district, { regenciesId: 200 });
    });

    it('getAllVillages always includes districtsId', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAllVillages(300).subscribe();

      expect(api.get).toHaveBeenCalledWith(DBO_URL.location.village, { districtsId: 300 });
    });

    it('getAllBranches calls the branch endpoint', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAllBranches().subscribe();

      expect(api.get).toHaveBeenCalledWith(DBO_URL.branch, {});
    });
  });

  describe('negative', () => {
    it('getAllProvinces propagates a transport error to the caller', (done) => {
      api.get.and.returnValue(throwError(() => new Error('network down')));

      service.getAllProvinces().subscribe({
        next: () => fail('expected an error'),
        error: (err) => {
          expect(err.message).toBe('network down');
          done();
        }
      });
    });

    it('getAllRegencies passes through a success:false response without throwing', (done) => {
      api.get.and.returnValue(of({ success: false, statusCode: 500, message: 'Internal error', data: null as any }));

      service.getAllRegencies(31).subscribe((res) => {
        expect(res.success).toBeFalse();
        done();
      });
    });
  });
});
