export interface Owner {
  id: number;
  fullName: string;
  email: string | null;
  phone: string | null;
  userId: number | null;
  createdAt: string;
}

export interface OwnerRequest {
  fullName: string;
  email: string | null;
  phone: string | null;
  userId: number | null;
}
