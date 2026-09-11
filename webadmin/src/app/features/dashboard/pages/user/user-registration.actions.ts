import { createAction, props } from '@ngrx/store';
import { BranchResponse, DistrictsResponse, ProvinceResponse, RegenciesResponse, VillagesResponse } from '../../../../shared/models/dbo-response';

export type DropdownName = 'branch' | 'province' | 'regency' | 'district' | 'village';

export const setDropdownQuery = createAction('[User Registration] Set dropdown query', props<{ name: DropdownName; query: string }>());
export const loadRegistrationOptions = createAction('[User Registration] Load options');
export const registrationOptionsLoaded = createAction('[User Registration] Options loaded', props<{
  branches: BranchResponse[]; provinces: ProvinceResponse[];
}>());
export const registrationOptionsFailed = createAction('[User Registration] Options failed', props<{ error: string }>());
export const setRegencies = createAction('[User Registration] Set regencies', props<{ items: RegenciesResponse[] }>());
export const setDistricts = createAction('[User Registration] Set districts', props<{ items: DistrictsResponse[] }>());
export const setVillages = createAction('[User Registration] Set villages', props<{ items: VillagesResponse[] }>());
export const setBranches = createAction('[User Registration] Set branches', props<{ items: BranchResponse[] }>());
export const setProvinces = createAction('[User Registration] Set provinces', props<{ items: ProvinceResponse[] }>());
