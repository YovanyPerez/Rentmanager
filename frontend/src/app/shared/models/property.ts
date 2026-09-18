export type PropertyStatus = 'AVAILABLE' | 'RENTED' | 'MAINTENANCE' | 'INACTIVE';

export interface Property {
  id: number;
  ownerId: number;
  ownerName: string;
  address: string;
  city: string;
  description: string | null;
  monthlyRent: number;
  status: PropertyStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyRequest {
  ownerId: number;
  address: string;
  city: string;
  description: string | null;
  monthlyRent: number;
}
