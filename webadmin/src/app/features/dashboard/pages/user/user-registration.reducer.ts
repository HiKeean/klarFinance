import { createReducer, on } from '@ngrx/store';
import { BranchResponse, DistrictsResponse, ProvinceResponse, RegenciesResponse, VillagesResponse } from '../../../../shared/models/dbo-response';
import * as RegistrationActions from './user-registration.actions';

export interface UserRegistrationState {
  branches: BranchResponse[]; provinces: ProvinceResponse[]; regencies: RegenciesResponse[];
  districts: DistrictsResponse[]; villages: VillagesResponse[];
  queries: Record<RegistrationActions.DropdownName, string>; loading: boolean; loaded: boolean; error: string;
}

export const initialUserRegistrationState: UserRegistrationState = {
  branches: [], provinces: [], regencies: [], districts: [], villages: [],
  queries: { branch: '', province: '', regency: '', district: '', village: '' },
  loading: false, loaded: false, error: '',
};

export const userRegistrationReducer = createReducer(
  initialUserRegistrationState,
  on(RegistrationActions.setDropdownQuery, (state, { name, query }) => ({ ...state, queries: { ...state.queries, [name]: query } })),
  on(RegistrationActions.loadRegistrationOptions, (state) => ({ ...state, loading: true, error: '' })),
  on(RegistrationActions.registrationOptionsLoaded, (state, { branches, provinces }) => ({ ...state, branches, provinces, loading: false, loaded: true })),
  on(RegistrationActions.registrationOptionsFailed, (state, { error }) => ({ ...state, loading: false, error })),
  on(RegistrationActions.setRegencies, (state, { items }) => ({ ...state, regencies: items })),
  on(RegistrationActions.setDistricts, (state, { items }) => ({ ...state, districts: items })),
  on(RegistrationActions.setVillages, (state, { items }) => ({ ...state, villages: items })),
  on(RegistrationActions.setBranches, (state, { items }) => ({ ...state, branches: items })),
  on(RegistrationActions.setProvinces, (state, { items }) => ({ ...state, provinces: items })),
);
