import { isRoleAllowed } from './allowed-roles.policy';

describe('isRoleAllowed', () => {
  it('[positive] allows CHECKER', () => {
    expect(isRoleAllowed('CHECKER')).toBe(true);
  });

  it('[positive] allows BM', () => {
    expect(isRoleAllowed('BM')).toBe(true);
  });

  it('[positive] is case-insensitive', () => {
    expect(isRoleAllowed('checker')).toBe(true);
    expect(isRoleAllowed('Bm')).toBe(true);
  });

  it('[negative] rejects an unrelated role', () => {
    expect(isRoleAllowed('SUPERADMIN')).toBe(false);
    expect(isRoleAllowed('NASABAH')).toBe(false);
  });

  it('[negative] rejects null/undefined/empty', () => {
    expect(isRoleAllowed(null)).toBe(false);
    expect(isRoleAllowed(undefined)).toBe(false);
    expect(isRoleAllowed('')).toBe(false);
  });
});
