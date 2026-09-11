import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { BranchTerritoryPage } from './branch-territory';
import { BranchTerritoryServices } from '../../services/branch-territory-services';
import { Dbo } from '../../../../core/services/dbo/dbo';
import { BranchTerritory, RegencyGap, BranchOption } from '../../models/branch-territory.model';

const territories: BranchTerritory[] = [
  { id: 1, branchId: 1000, branchName: 'Cabang Jakarta Pusat', regencyId: 3171, regencyName: 'Jakarta Pusat', provinceName: 'DKI Jakarta' }
];
const gaps: RegencyGap[] = [{ regencyId: 9999, regencyName: 'Regency Tanpa Branch', provinceName: 'Jawa Barat' }];
const branchOptions: BranchOption[] = [{ id: 1000, name: 'Cabang Jakarta Pusat' }];

describe('BranchTerritoryPage', () => {
  let fixture: ComponentFixture<BranchTerritoryPage>;
  let component: BranchTerritoryPage;
  let territoryServices: jasmine.SpyObj<BranchTerritoryServices>;
  let dbo: jasmine.SpyObj<Dbo>;

  beforeEach(async () => {
    territoryServices = jasmine.createSpyObj<BranchTerritoryServices>('BranchTerritoryServices', [
      'getAll', 'getGaps', 'assignRegency', 'assignProvince', 'unassign', 'listBranches'
    ]);
    dbo = jasmine.createSpyObj<Dbo>('Dbo', ['getAllProvinces', 'getAllRegencies']);

    territoryServices.getAll.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: territories }));
    territoryServices.getGaps.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: gaps }));
    // openAssign() always fetches branches/provinces (lazily, once) - default them so any test
    // that opens the assign modal doesn't crash; tests that care about the fetch itself override these.
    territoryServices.listBranches.and.returnValue(of(branchOptions));
    dbo.getAllProvinces.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [{ id: 32, name: 'Jawa Barat' }, { id: 73, name: 'Sulawesi Selatan' }] as any }));
    dbo.getAllRegencies.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [{ id: 3273, name: 'Kota Bandung' } as any] }));

    await TestBed.configureTestingModule({
      imports: [BranchTerritoryPage],
      providers: [
        { provide: BranchTerritoryServices, useValue: territoryServices },
        { provide: Dbo, useValue: dbo }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BranchTerritoryPage);
    component = fixture.componentInstance;
  });

  it('[positive] loads territories and coverage gaps on init', () => {
    fixture.detectChanges();

    expect(component.territories()).toEqual(territories);
    expect(component.gaps()).toEqual(gaps);
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(territories.length);
  });

  it('[negative] shows an error and empties the list when loading territories fails', () => {
    territoryServices.getAll.and.returnValue(of({ success: false, statusCode: 500, message: 'Gagal memuat branch territory.', data: null as any }));

    fixture.detectChanges();

    expect(component.territories()).toEqual([]);
    expect(component.errorMessage()).toBe('Gagal memuat branch territory.');
  });

  it('[negative] a transport error while loading gaps leaves the gap list empty without crashing the page', () => {
    territoryServices.getGaps.and.returnValue(throwError(() => new Error('boom')));

    fixture.detectChanges();

    expect(component.gaps()).toEqual([]);
    expect(component.territories()).toEqual(territories);
  });

  describe('province classification (UX-only, backend is still source of truth)', () => {
    it('[positive] a Java province (non-Jakarta) requires a regency pick before submit is enabled', () => {
      fixture.detectChanges();
      component.openAssign();
      component.selectedBranchId.set(1000);
      component.onProvinceChange('32'); // Jawa Barat

      expect(component.isJavaProvince()).toBeTrue();
      expect(component.isJakarta()).toBeFalse();
      expect(component.canSubmitAssign()).toBeFalse();

      component.selectedRegencyId.set(3273);
      expect(component.canSubmitAssign()).toBeTrue();
    });

    it('[positive] an outside-Java province can submit as soon as a branch is picked (province-level bulk assign)', () => {
      fixture.detectChanges();
      component.openAssign();
      component.selectedBranchId.set(1000);
      component.onProvinceChange('73'); // Sulawesi Selatan (not in the Java set)

      expect(component.isJavaProvince()).toBeFalse();
      expect(component.canSubmitAssign()).toBeTrue();
    });
  });

  describe('submitAssign', () => {
    it('[positive] a Java province calls assignRegency and refreshes both lists', () => {
      fixture.detectChanges();
      territoryServices.assignRegency.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: territories[0] }));
      territoryServices.getAll.calls.reset();
      territoryServices.getGaps.calls.reset();

      component.openAssign();
      component.selectedBranchId.set(1000);
      component.onProvinceChange('32');
      component.selectedRegencyId.set(3273);
      component.submitAssign();

      expect(territoryServices.assignRegency).toHaveBeenCalledWith(1000, 3273);
      expect(component.assignModalOpen).toBeFalse();
      expect(territoryServices.getAll).toHaveBeenCalled();
      expect(territoryServices.getGaps).toHaveBeenCalled();
    });

    it('[positive] a non-Java province calls assignProvince (bulk) instead of assignRegency', () => {
      fixture.detectChanges();
      territoryServices.assignProvince.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: territories }));

      component.openAssign();
      component.selectedBranchId.set(1000);
      component.onProvinceChange('73');
      component.submitAssign();

      expect(territoryServices.assignProvince).toHaveBeenCalledWith(1000, 73);
      expect(territoryServices.assignRegency).not.toHaveBeenCalled();
    });

    it('[negative] surfaces the backend error (e.g. regency already has a branch) and keeps the modal open', () => {
      fixture.detectChanges();
      territoryServices.assignRegency.and.returnValue(of({ success: false, statusCode: 409, message: 'Regency already has a branch', data: null as any }));

      component.openAssign();
      component.selectedBranchId.set(1000);
      component.onProvinceChange('32');
      component.selectedRegencyId.set(3273);
      component.submitAssign();

      expect(component.assignError()).toBe('Regency already has a branch');
      expect(component.assignModalOpen).toBeTrue();
    });

    it('[negative] a transport error also surfaces via assignError, not an unhandled rejection', () => {
      fixture.detectChanges();
      territoryServices.assignRegency.and.returnValue(throwError(() => ({ error: { message: 'Server unreachable' } })));

      component.openAssign();
      component.selectedBranchId.set(1000);
      component.onProvinceChange('32');
      component.selectedRegencyId.set(3273);
      component.submitAssign();

      expect(component.assignError()).toBe('Server unreachable');
      expect(component.isSaving()).toBeFalse();
    });

    it('[negative] does nothing when branch or province is not yet selected', () => {
      fixture.detectChanges();
      component.openAssign();

      component.submitAssign();

      expect(territoryServices.assignRegency).not.toHaveBeenCalled();
      expect(territoryServices.assignProvince).not.toHaveBeenCalled();
    });
  });

  describe('remove', () => {
    it('[positive] unassigns after confirmation and reloads the list', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(true);
      territoryServices.unassign.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));
      territoryServices.getAll.calls.reset();

      component.remove(territories[0]);

      expect(territoryServices.unassign).toHaveBeenCalledWith(1);
      expect(territoryServices.getAll).toHaveBeenCalled();
    });

    it('[negative] does not call the API when the confirmation is cancelled', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(false);

      component.remove(territories[0]);

      expect(territoryServices.unassign).not.toHaveBeenCalled();
    });
  });

  it('[positive] openAssign lazily loads branches/provinces only the first time it opens', () => {
    fixture.detectChanges();

    component.openAssign();
    component.openAssign();

    expect(territoryServices.listBranches).toHaveBeenCalledTimes(1);
    expect(dbo.getAllProvinces).toHaveBeenCalledTimes(1);
  });
});
