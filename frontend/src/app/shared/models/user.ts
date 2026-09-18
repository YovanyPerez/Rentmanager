import { AppRole } from './auth';

export interface UserSummary {
  id: number;
  email: string;
  fullName: string;
  role: AppRole;
  active: boolean;
}
