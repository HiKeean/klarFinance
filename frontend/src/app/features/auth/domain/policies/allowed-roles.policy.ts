/**
 * Aplikasi Checker & BM cuma boleh diakses dua role ini — role lain (mis. SUPERADMIN,
 * NASABAH) valid secara auth di backend tapi tidak berhak pakai app ini.
 */
const ALLOWED_ROLES = ['CHECKER', 'BM'] as const;

export function isRoleAllowed(role: string | null | undefined): boolean {
  if (!role) return false;
  return (ALLOWED_ROLES as readonly string[]).includes(role.toUpperCase());
}
