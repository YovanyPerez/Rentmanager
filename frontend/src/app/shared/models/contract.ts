export type ContractStatus = 'DRAFT' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED';

export interface Contract {
  id: number;
  propertyId: number;
  propertyAddress: string;
  tenantId: number;
  tenantName: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  status: ContractStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ContractRequest {
  propertyId: number;
  tenantId: number;
  startDate: string;
  endDate: string;
  monthlyRent: number;
}
