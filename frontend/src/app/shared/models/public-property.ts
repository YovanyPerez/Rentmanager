import { CurrencyCode } from './currency';
import { PropertyImage } from './property';

export interface PublicProperty {
  id: number;
  address: string;
  city: string;
  description: string | null;
  imageUrl: string | null;
  images: PropertyImage[];
  monthlyRent: number;
  currency: CurrencyCode;
  status: 'AVAILABLE';
}
