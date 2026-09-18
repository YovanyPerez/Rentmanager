import { CurrencyCode } from './currency';

export interface CurrencyTotal {
  currency: CurrencyCode;
  total: number;
}

export interface DashboardStats {
  totalProperties: number;
  availableProperties: number;
  rentedProperties: number;
  activeContracts: number;
  pendingPayments: number;
  overduePayments: number;
  openMaintenanceRequests: number;
  monthlyIncome: CurrencyTotal[];
}
