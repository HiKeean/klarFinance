export interface InquiryResult {
  id: number;
  applicationCode: string | null;
  customerName: string | null;
  customerIdentity: string;
  status: string;
}
