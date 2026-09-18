export type PaymentStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface Payment {
  id: number;
  contractId: number;
  propertyId: number;
  propertyAddress: string;
  tenantName: string;
  amount: number;
  dueDate: string;
  paidDate: string | null;
  status: PaymentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentRequest {
  contractId: number;
  amount: number;
  dueDate: string;
}

export interface PaymentUpdateRequest {
  status: PaymentStatus;
  paidDate?: string;
}
