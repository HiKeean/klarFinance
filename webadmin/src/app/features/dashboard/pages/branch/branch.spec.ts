import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { BranchPage } from './branch';
import { BranchServices } from '../../services/branch-services';
import { BranchListItem, BmOption } from '../../models/branch.model';

const branches: BranchListItem[] = [
  { branchCode: 1, name: 'HO', address: 'Jl. A', villageName: 'Menteng', bmIdentity: null, bmName: null },
  { branchCode: 2, name: 'Cabang 2', address: null, villageName: null, bmIdentity: null, bmName: null }
];
const bms: BmOption[] = [{ identity: '2026001', name: 'Budi', branchCode: 1, branchName: 'HO' }];

describe('BranchPage', () => {
  let fixture: ComponentFixture<BranchPage>;
  let component: BranchPage;
  let branchServices: jasmine.SpyObj<BranchServices>;

  beforeEach(async () => {
    branchServices = jasmine.createSpyObj<BranchServices>('BranchServices', ['listBranches', 'listBms', 'assignBranch']);
    branchServices.listBranches.and.returnValue(of(branches));
    branchServices.listBms.and.returnValue(of(bms));

    await TestBed.configureTestingModule({
      imports: [BranchPage],
      providers: [{ provide: BranchServices, useValue: branchServices }]
    }).compileComponents();

    fixture = TestBed.createComponent(BranchPage);
    component = fixture.componentInstance;
  });

  it('[positive] joins branches with their BM on load', () => {
    fixture.detectChanges();

    expect(component.branches()).toEqual([
      { ...branches[0], bmIdentity: '2026001', bmName: 'Budi' },
      branches[1]
    ]);
    expect(component.isLoading()).toBeFalse();
  });

  it('[negative] clears the list and sets an error message when loading fails', () => {
    branchServices.listBranches.and.returnValue(throwError(() => new Error('boom')));

    fixture.detectChanges();

    expect(component.branches()).toEqual([]);
    expect(component.errorMessage()).toBe('Gagal memuat daftar branch.');
    expect(component.isLoading()).toBeFalse();
  });

  describe('bmOptions', () => {
    it('[positive] includes the BM current branch in the label when already assigned', () => {
      fixture.detectChanges();
      expect(component.bmOptions()).toEqual([{ label: 'Budi (2026001) — saat ini: HO', value: '2026001' }]);
    });

    it('[positive] omits the "saat ini" suffix when the BM has no branch', () => {
      branchServices.listBms.and.returnValue(of([{ identity: '2026002', name: 'Siti', branchCode: null, branchName: null }]));
      fixture.detectChanges();

      expect(component.bmOptions()).toEqual([{ label: 'Siti (2026002)', value: '2026002' }]);
    });
  });

  describe('openAssign / closeAssign', () => {
    it('[positive] openAssign seeds the modal with the branch and its current BM', () => {
      fixture.detectChanges();

      component.openAssign({ ...branches[0], bmIdentity: '2026001', bmName: 'Budi' });

      expect(component.activeBranch()?.branchCode).toBe(1);
      expect(component.selectedBmIdentity()).toBe('2026001');
      expect(component.assignOpen()).toBeTrue();
    });

    it('[negative] closeAssign does nothing while saving', () => {
      fixture.detectChanges();
      component.openAssign(branches[0]);
      component.isSaving.set(true);

      component.closeAssign();

      expect(component.assignOpen()).toBeTrue();
    });
  });

  describe('submitAssign', () => {
    it('[positive] assigns the BM, closes the modal and reloads', () => {
      fixture.detectChanges();
      component.openAssign(branches[1]);
      component.selectedBmIdentity.set('2026001');
      branchServices.assignBranch.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));
      branchServices.listBranches.calls.reset();

      component.submitAssign();

      expect(branchServices.assignBranch).toHaveBeenCalledWith('2026001', 2);
      expect(component.assignOpen()).toBeFalse();
      expect(branchServices.listBranches).toHaveBeenCalled();
    });

    it('[negative] surfaces the backend error and keeps the modal open', () => {
      fixture.detectChanges();
      component.openAssign(branches[1]);
      component.selectedBmIdentity.set('2026001');
      branchServices.assignBranch.and.returnValue(of({ success: false, statusCode: 409, message: 'Sudah ada BM', data: null }));

      component.submitAssign();

      expect(component.assignError()).toBe('Sudah ada BM');
      expect(component.assignOpen()).toBeTrue();
      expect(component.isSaving()).toBeFalse();
    });

    it('[negative] surfaces a transport error message from err.error.message', () => {
      fixture.detectChanges();
      component.openAssign(branches[1]);
      component.selectedBmIdentity.set('2026001');
      branchServices.assignBranch.and.returnValue(throwError(() => ({ error: { message: 'Server down' } })));

      component.submitAssign();

      expect(component.assignError()).toBe('Server down');
      expect(component.isSaving()).toBeFalse();
    });

    it('[negative] does nothing when no BM is selected', () => {
      fixture.detectChanges();
      component.openAssign(branches[1]);
      component.selectedBmIdentity.set(null);

      component.submitAssign();

      expect(branchServices.assignBranch).not.toHaveBeenCalled();
    });
  });

  describe('unassign', () => {
    it('[positive] unassigns after confirmation and reloads', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(true);
      branchServices.assignBranch.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      component.unassign({ ...branches[0], bmIdentity: '2026001', bmName: 'Budi' });

      expect(branchServices.assignBranch).toHaveBeenCalledWith('2026001', null);
    });

    it('[negative] does nothing when the branch has no BM assigned', () => {
      fixture.detectChanges();
      const confirmSpy = spyOn(window, 'confirm');

      component.unassign(branches[1]);

      expect(confirmSpy).not.toHaveBeenCalled();
      expect(branchServices.assignBranch).not.toHaveBeenCalled();
    });

    it('[negative] does nothing when the user cancels the confirmation', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(false);

      component.unassign({ ...branches[0], bmIdentity: '2026001', bmName: 'Budi' });

      expect(branchServices.assignBranch).not.toHaveBeenCalled();
    });
  });
});
