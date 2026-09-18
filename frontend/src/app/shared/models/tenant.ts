export interface Tenant {
  id: number;
  fullName: string;
  email: string | null;
  phone: string | null;
  userId: number | null;
  createdAt: string;
}

export interface TenantRequest {
  fullName: string;
  email: string | null;
  phone: string | null;
  userId: number | null;
}
