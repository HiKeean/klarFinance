import { userRegistrationReducer, initialUserRegistrationState, UserRegistrationState } from './user-registration.reducer';
import * as RegistrationActions from './user-registration.actions';
import { BranchResponse, ProvinceResponse, RegenciesResponse, DistrictsResponse, VillagesResponse } from '../../../../shared/models/dbo-response';

describe('userRegistrationReducer', () => {
  it('[positive] returns the initial state for an unknown action', () => {
    const state = userRegistrationReducer(undefined, { type: '[Unknown] noop' });
    expect(state).toEqual(initialUserRegistrationState);
  });

  describe('setDropdownQuery', () => {
    it('[positive] updates only the given dropdown query and leaves the others untouched', () => {
      const state = userRegistrationReducer(initialUserRegistrationState, RegistrationActions.setDropdownQuery({ name: 'province', query: 'jawa' }));

      expect(state.queries).toEqual({ branch: '', province: 'jawa', regency: '', district: '', village: '' });
      // Unrelated slices are unchanged.
      expect(state.branches).toBe(initialUserRegistrationState.branches);
    });
  });

  describe('loadRegistrationOptions / registrationOptionsLoaded / registrationOptionsFailed', () => {
    it('[positive] sets loading true and clears any previous error on load', () => {
      const withError: UserRegistrationState = { ...initialUserRegistrationState, error: 'previous error' };

      const state = userRegistrationReducer(withError, RegistrationActions.loadRegistrationOptions());

      expect(state.loading).toBeTrue();
      expect(state.error).toBe('');
    });

    it('[positive] stores branches/provinces and marks loaded on success', () => {
      const branches: BranchResponse[] = [{ branchCode: 1, name: 'HO' }];
      const provinces: ProvinceResponse[] = [{ id: 1, name: 'Jawa Barat' }];
      const loading = { ...initialUserRegistrationState, loading: true };

      const state = userRegistrationReducer(loading, RegistrationActions.registrationOptionsLoaded({ branches, provinces }));

      expect(state.branches).toEqual(branches);
      expect(state.provinces).toEqual(provinces);
      expect(state.loading).toBeFalse();
      expect(state.loaded).toBeTrue();
    });

    it('[negative] stores the error message and clears loading on failure without marking loaded', () => {
      const loading = { ...initialUserRegistrationState, loading: true };

      const state = userRegistrationReducer(loading, RegistrationActions.registrationOptionsFailed({ error: 'network down' }));

      expect(state.loading).toBeFalse();
      expect(state.loaded).toBeFalse();
      expect(state.error).toBe('network down');
    });
  });

  describe('cascading location setters', () => {
    it('[positive] setRegencies replaces regencies only', () => {
      const items: RegenciesResponse[] = [{ id: 1, name: 'Bandung', province: { id: 1, name: 'Jawa Barat' } }];
      const state = userRegistrationReducer(initialUserRegistrationState, RegistrationActions.setRegencies({ items }));
      expect(state.regencies).toEqual(items);
      expect(state.districts).toEqual([]);
    });

    it('[positive] setDistricts replaces districts only', () => {
      const items: DistrictsResponse[] = [{ id: 1, name: 'Coblong', regency: { id: 1, name: 'Bandung', province: { id: 1, name: 'Jawa Barat' } } }];
      const state = userRegistrationReducer(initialUserRegistrationState, RegistrationActions.setDistricts({ items }));
      expect(state.districts).toEqual(items);
    });

    it('[positive] setVillages replaces villages only', () => {
      const items: VillagesResponse[] = [{ id: 1, name: 'Dago', district: null }];
      const state = userRegistrationReducer(initialUserRegistrationState, RegistrationActions.setVillages({ items }));
      expect(state.villages).toEqual(items);
    });

    it('[positive] setBranches replaces branches only', () => {
      const items: BranchResponse[] = [{ branchCode: 2, name: 'Branch 2' }];
      const state = userRegistrationReducer(initialUserRegistrationState, RegistrationActions.setBranches({ items }));
      expect(state.branches).toEqual(items);
    });

    it('[positive] setProvinces replaces provinces only', () => {
      const items: ProvinceResponse[] = [{ id: 2, name: 'Jawa Timur' }];
      const state = userRegistrationReducer(initialUserRegistrationState, RegistrationActions.setProvinces({ items }));
      expect(state.provinces).toEqual(items);
    });
  });

  it('[positive] does not mutate the previous state object (immutability)', () => {
    const prev = initialUserRegistrationState;
    const next = userRegistrationReducer(prev, RegistrationActions.setDropdownQuery({ name: 'village', query: 'x' }));

    expect(next).not.toBe(prev);
    expect(prev.queries.village).toBe('');
  });
});
