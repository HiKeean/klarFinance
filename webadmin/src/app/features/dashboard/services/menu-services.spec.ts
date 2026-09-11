import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { MenuServices } from './menu-services';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';

describe('MenuServices', () => {
  let service: MenuServices;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'delete']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    service = TestBed.inject(MenuServices);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('positive', () => {
    it('getAllMenus calls the menus endpoint', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAllMenus().subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.auth.getAllMenus);
    });

    it('saveMenu posts name/url/logo', () => {
      api.post.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.saveMenu('Approval (BM)', '/bm/approval', 'fact_check').subscribe();

      expect(api.post).toHaveBeenCalledWith(SUPERADMIN_URL.auth.saveMenu, { name: 'Approval (BM)', url: '/bm/approval', logo: 'fact_check' });
    });

    it('updateMenu PUTs the new name to the menu-specific URL', () => {
      api.put.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.updateMenu(7, 'Inquiry (BM)').subscribe();

      expect(api.put).toHaveBeenCalledWith(SUPERADMIN_URL.auth.updateMenu(7), { menu: 'Inquiry (BM)' });
    });

    it('deleteMenu calls the menu-specific delete URL', () => {
      api.delete.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.deleteMenu(7).subscribe();

      expect(api.delete).toHaveBeenCalledWith(SUPERADMIN_URL.auth.deleteMenu(7));
    });
  });

  describe('negative', () => {
    it('getAllMenus propagates a transport error', (done) => {
      api.get.and.returnValue(throwError(() => new Error('boom')));

      service.getAllMenus().subscribe({
        next: () => fail('expected an error'),
        error: (err) => {
          expect(err.message).toBe('boom');
          done();
        }
      });
    });

    it('saveMenu passes through a success:false (duplicate URL) response', (done) => {
      api.post.and.returnValue(of({ success: false, statusCode: 409, message: 'URL already used', data: null }));

      service.saveMenu('Approval', '/approval', 'insert_chart').subscribe((res) => {
        expect(res.success).toBeFalse();
        expect(res.message).toBe('URL already used');
        done();
      });
    });
  });
});
