import { environment } from '../../../environments/environment';

const BASE_INTERNAL = `${environment.api.baseUrl}/internal/`;
const BASE_AUTH = `${environment.api.baseUrl}/auth/`;

export const AUTH_URL = {
  passwordResetRequest: `${BASE_AUTH}password-reset-requests`
} as const;

export const INTERNAL_URL = {
  auth: {
    login: `${BASE_INTERNAL}auth/login`
  },
  los: {
    dashboardSummary: `${BASE_INTERNAL}los/dashboard-summary`,
    limitApplications: `${BASE_INTERNAL}los/limit-applications`,
    limitApplicationDetail: (id: number) => `${BASE_INTERNAL}los/limit-applications/${id}`,
    limitApplicationPicture: (id: number, type: 'ktp' | 'kyc') => `${BASE_INTERNAL}los/limit-applications/${id}/picture/${type}`,
    checkerDecision: (id: number) => `${BASE_INTERNAL}los/limit-applications/${id}/checker-decision`,
    bmDecision: (id: number) => `${BASE_INTERNAL}los/limit-applications/${id}/bm-decision`,
    checkerBreak: `${BASE_INTERNAL}los/checker/break`,
    limitApplicationsSearch: `${BASE_INTERNAL}los/limit-applications/search`
  },
  fin: {
    loanReviewDetail: (id: number) => `${BASE_INTERNAL}fin/loan-reviews/${id}`,
    loanReviewBmDecision: (id: number) => `${BASE_INTERNAL}fin/loan-reviews/${id}/bm-decision`
  }
} as const;
