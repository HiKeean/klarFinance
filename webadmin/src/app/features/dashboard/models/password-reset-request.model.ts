export interface PasswordResetRequestItem {
  id: number;
  identity: string;
  name: string | null;
  role: string | null;
  requestedAt: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  decidedAt: string | null;
  decidedBy: string | null;
  reason: string | null;
}
