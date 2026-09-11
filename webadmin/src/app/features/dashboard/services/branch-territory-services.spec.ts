import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { BranchTerritoryServices } from './branch-territory-services';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL, DBO_URL } from '../../../core/config/url';

describe('BranchTerritoryServices', () => {
  let service: BranchTerritoryServices;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'delete']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    service = TestBed.inject(BranchTerritoryServices);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('positive', () => {
    it('getAll calls the endpoint without a branchId param when none is given', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAll().subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.dbo.branchTerritory, undefined);
    });

    it('getAll includes branchId when given', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAll(42).subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.dbo.branchTerritory, { branchId: 42 });
    });

    it('assignRegency posts branchId+regencyId to the regency-assign endpoint', () => {
      api.post.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.assignRegency(1, 200).subscribe();

      expect(api.post).toHaveBeenCalledWith(SUPERADMIN_URL.dbo.assignRegencyTerritory, { branchId: 1, regencyId: 200 });
    });

    it('assignProvince posts branchId+provinceId to the province-assign (bulk) endpoint', () => {
      api.post.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.assignProvince(1, 15).subscribe();

      expect(api.post).toHaveBeenCalledWith(SUPERADMIN_URL.dbo.assignProvinceTerritory, { branchId: 1, provinceId: 15 });
    });

    it('unassign calls the id-specific delete URL', () => {
      api.delete.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.unassign(9).subscribe();

      expect(api.delete).toHaveBeenCalledWith(SUPERADMIN_URL.dbo.deleteBranchTerritory(9));
    });

    it('listBranches unwraps the paginated {content:[...]} envelope into BranchOption[]', (done) => {
      api.get.and.returnValue(of({
        success: true,
        statusCode: 200,
        message: 'OK',
        data: { content: [{ branchCode: 1000, name: 'Head Office' }, { branchCode: 1001, name: 'Cabang Jakarta' }] }
      }));

      service.listBranches().subscribe((branches) => {
        expect(branches).toEqual([{ id: 1000, name: 'Head Office' }, { id: 1001, name: 'Cabang Jakarta' }]);
        expect(api.get).toHaveBeenCalledWith(DBO_URL.branch, { page: 0, size: 200 });
        done();
      });
    });

    it('listBranches forwards the name filter as a search param', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: { content: [] } }));

      service.listBranches('Jakarta').subscribe();

      expect(api.get).toHaveBeenCalledWith(DBO_URL.branch, { page: 0, size: 200, name: 'Jakarta' });
    });
  });

  describe('negative', () => {
    it('getGaps propagates a transport error', (done) => {
      api.get.and.returnValue(throwError(() => new Error('boom')));

      service.getGaps().subscribe({
        next: () => fail('expected an error'),
        error: (err) => {
          expect(err.message).toBe('boom');
          done();
        }
      });
    });

    it('listBranches throws the backend message when response.success is false', (done) => {
      api.get.and.returnValue(of({ success: false, statusCode: 500, message: 'Gagal memuat branch', data: null as any }));

      service.listBranches().subscribe({
        next: () => fail('expected an error'),
        error: (err) => {
          expect(err.message).toBe('Gagal memuat branch');
          done();
        }
      });
    });

    it('assignRegency passes through a success:false (limit exceeded) response instead of throwing', (done) => {
      api.post.and.returnValue(of({ success: false, statusCode: 409, message: 'Regency already has a branch', data: null }));

      service.assignRegency(1, 200).subscribe((res) => {
        expect(res.success).toBeFalse();
        expect(res.message).toBe('Regency already has a branch');
        done();
      });
    });
  });
});
