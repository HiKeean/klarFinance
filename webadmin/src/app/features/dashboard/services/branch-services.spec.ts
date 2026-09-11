import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { BranchServices } from './branch-services';
import { ApiService } from '../../../core/services/api.service';
import { DBO_URL, SUPERADMIN_URL } from '../../../core/config/url';

describe('BranchServices', () => {
  let service: BranchServices;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'delete', 'patch']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    service = TestBed.inject(BranchServices);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listBranches', () => {
    it('[positive] maps the paginated branch response into flat BranchListItem[] with nulled BM fields', (done) => {
      api.get.and.returnValue(
        of({
          success: true,
          statusCode: 200,
          message: 'OK',
          data: {
            content: [
              { branchCode: 1, name: 'HO', address: 'Jl. A', village: { name: 'Menteng' } },
              { branchCode: 2, name: 'Cabang 2', address: null, village: null }
            ]
          }
        })
      );

      service.listBranches().subscribe((branches) => {
        expect(api.get).toHaveBeenCalledWith(DBO_URL.branch, { page: 0, size: 200 });
        expect(branches).toEqual([
          { branchCode: 1, name: 'HO', address: 'Jl. A', villageName: 'Menteng', bmIdentity: null, bmName: null },
          { branchCode: 2, name: 'Cabang 2', address: null, villageName: null, bmIdentity: null, bmName: null }
        ]);
        done();
      });
    });

    it('[negative] throws when the backend responds success:false', (done) => {
      api.get.and.returnValue(of({ success: false, statusCode: 500, message: 'Gagal memuat branch', data: { content: [] } }));

      service.listBranches().subscribe({
        next: () => fail('expected an error'),
        error: (err: Error) => {
          expect(err.message).toBe('Gagal memuat branch');
          done();
        }
      });
    });

    it('[negative] propagates a transport error', (done) => {
      api.get.and.returnValue(throwError(() => new Error('network down')));

      service.listBranches().subscribe({
        next: () => fail('expected an error'),
        error: (err: Error) => {
          expect(err.message).toBe('network down');
          done();
        }
      });
    });
  });

  describe('listBms', () => {
    it('[positive] maps the paginated user response into BmOption[], defaulting missing identity/branch', (done) => {
      api.get.and.returnValue(
        of({
          success: true,
          statusCode: 200,
          message: 'OK',
          data: {
            content: [
              { identity: '2026001', name: 'Budi', branch: { branchCode: 1, name: 'HO' } },
              { identity: null, name: 'Siti', branch: null }
            ]
          }
        })
      );

      service.listBms().subscribe((bms) => {
        expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.auth.getAllUsers, { page: 0, size: 500, role: 'BM' });
        expect(bms).toEqual([
          { identity: '2026001', name: 'Budi', branchCode: 1, branchName: 'HO' },
          { identity: '', name: 'Siti', branchCode: null, branchName: null }
        ]);
        done();
      });
    });

    it('[negative] throws when the backend responds success:false', (done) => {
      api.get.and.returnValue(of({ success: false, statusCode: 500, message: 'Gagal memuat BM', data: { content: [] } }));

      service.listBms().subscribe({
        next: () => fail('expected an error'),
        error: (err: Error) => {
          expect(err.message).toBe('Gagal memuat BM');
          done();
        }
      });
    });
  });

  describe('assignBranch', () => {
    it('[positive] PATCHes the branchId onto the BM identity endpoint', () => {
      api.patch.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.assignBranch('2026001', 5).subscribe();

      expect(api.patch).toHaveBeenCalledWith(SUPERADMIN_URL.auth.assignBranch('2026001'), { branchId: 5 });
    });

    it('[positive] passes branchId: null to unassign', () => {
      api.patch.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.assignBranch('2026001', null).subscribe();

      expect(api.patch).toHaveBeenCalledWith(SUPERADMIN_URL.auth.assignBranch('2026001'), { branchId: null });
    });
  });
});
