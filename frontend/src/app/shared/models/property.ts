import { CurrencyCode } from './currency';

export type PropertyStatus = 'AVAILABLE' | 'RENTED' | 'MAINTENANCE' | 'INACTIVE';

export interface PropertyImage {
  id: number;
  url: string;
  position: number;
}

export interface Property {
  id: number;
  ownerId: number;
  ownerName: string;
  address: string;
  city: string;
  description: string | null;
  imageUrl: string | null;
  images: PropertyImage[];
  monthlyRent: number;
  currency: CurrencyCode;
  status: PropertyStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyRequest {
  ownerId: number;
  address: string;
  city: string;
  description: string | null;
  currency: CurrencyCode;
  monthlyRent: number;
}
