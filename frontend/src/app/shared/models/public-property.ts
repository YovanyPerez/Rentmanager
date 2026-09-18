export interface PublicProperty {
  id: number;
  address: string;
  city: string;
  description: string | null;
  imageUrl: string | null;
  monthlyRent: number;
  status: 'AVAILABLE';
}
