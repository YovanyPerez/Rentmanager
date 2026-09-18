export type MaintenanceStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface Maintenance {
  id: number;
  propertyId: number;
  propertyAddress: string;
  createdById: number;
  createdByName: string;
  title: string;
  description: string | null;
  status: MaintenanceStatus;
  assignedTo: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MaintenanceCreateRequest {
  propertyId: number;
  title: string;
  description: string | null;
}

export interface MaintenanceUpdateRequest {
  status: MaintenanceStatus;
  assignedTo?: string | null;
}
