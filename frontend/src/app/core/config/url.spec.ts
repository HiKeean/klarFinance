import { AUTH_URL, INTERNAL_URL } from './url';

describe('url config', () => {
  it('[positive] AUTH_URL.passwordResetRequest points at the auth password-reset-requests endpoint', () => {
    expect(AUTH_URL.passwordResetRequest).toContain('/auth/password-reset-requests');
  });

  it('[positive] INTERNAL_URL.auth.login points at the internal auth login endpoint', () => {
    expect(INTERNAL_URL.auth.login).toContain('/internal/auth/login');
  });

  it('[positive] limitApplicationDetail interpolates the id', () => {
    expect(INTERNAL_URL.los.limitApplicationDetail(42)).toContain('/limit-applications/42');
  });

  it('[positive] limitApplicationPicture interpolates the id and type', () => {
    expect(INTERNAL_URL.los.limitApplicationPicture(7, 'ktp')).toContain('/limit-applications/7/picture/ktp');
    expect(INTERNAL_URL.los.limitApplicationPicture(7, 'kyc')).toContain('/limit-applications/7/picture/kyc');
  });

  it('[positive] checkerDecision and bmDecision interpolate the id', () => {
    expect(INTERNAL_URL.los.checkerDecision(3)).toContain('/limit-applications/3/checker-decision');
    expect(INTERNAL_URL.los.bmDecision(3)).toContain('/limit-applications/3/bm-decision');
  });

  it('[positive] loanReviewDetail and loanReviewBmDecision interpolate the id', () => {
    expect(INTERNAL_URL.fin.loanReviewDetail(9)).toContain('/fin/loan-reviews/9');
    expect(INTERNAL_URL.fin.loanReviewBmDecision(9)).toContain('/fin/loan-reviews/9/bm-decision');
  });
});
