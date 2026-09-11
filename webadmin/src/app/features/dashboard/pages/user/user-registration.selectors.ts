import { createFeatureSelector, createSelector } from '@ngrx/store';
import { UserRegistrationState } from './user-registration.reducer';

export const selectUserRegistration = createFeatureSelector<UserRegistrationState>('userRegistration');
export const selectBranches = createSelector(selectUserRegistration, (state) => state.branches);
export const selectProvinces = createSelector(selectUserRegistration, (state) => state.provinces);
export const selectRegencies = createSelector(selectUserRegistration, (state) => state.regencies);
export const selectDistricts = createSelector(selectUserRegistration, (state) => state.districts);
export const selectVillages = createSelector(selectUserRegistration, (state) => state.villages);
export const selectRegistrationLoading = createSelector(selectUserRegistration, (state) => state.loading);
export const selectRegistrationLoaded = createSelector(selectUserRegistration, (state) => state.loaded);
export const selectRegistrationError = createSelector(selectUserRegistration, (state) => state.error);
export const selectDropdownQueries = createSelector(selectUserRegistration, (state) => state.queries);
