import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'landing.title',
    data: { public: true },
    loadComponent: () => import('./features/public/landing/landing').then((m) => m.Landing),
  },
  {
    path: 'search',
    title: 'search.title',
    data: { public: true },
    loadComponent: () => import('./features/public/search/public-search').then((m) => m.PublicSearch),
  },
  {
    path: 'property/:id',
    title: 'detail.title',
    data: { public: true },
    loadComponent: () =>
      import('./features/public/detail/property-detail').then((m) => m.PropertyDetail),
  },
  {
    path: 'login',
    title: 'auth.login.title',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    title: 'auth.register.title',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'home',
    title: 'home.title',
    canActivate: [authGuard],
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
  },
  {
    path: 'dashboard',
    title: 'dashboard.title',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'properties',
    title: 'properties.title',
    canActivate: [authGuard, roleGuard('ADMIN', 'OWNER')],
    loadComponent: () =>
      import('./features/properties/list/properties-list').then((m) => m.PropertiesList),
  },
  {
    path: 'properties/new',
    title: 'properties.new',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/properties/form/property-form').then((m) => m.PropertyForm),
  },
  {
    path: 'properties/:id/edit',
    title: 'properties.edit',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/properties/form/property-form').then((m) => m.PropertyForm),
  },
  {
    path: 'owners',
    title: 'owners.title',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/owners/list/owners-list').then((m) => m.OwnersList),
  },
  {
    path: 'owners/new',
    title: 'owners.new',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/owners/form/owner-form').then((m) => m.OwnerForm),
  },
  {
    path: 'owners/:id/edit',
    title: 'owners.edit',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/owners/form/owner-form').then((m) => m.OwnerForm),
  },
  {
    path: 'tenants',
    title: 'tenants.title',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/tenants/list/tenants-list').then((m) => m.TenantsList),
  },
  {
    path: 'tenants/new',
    title: 'tenants.new',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/tenants/form/tenant-form').then((m) => m.TenantForm),
  },
  {
    path: 'tenants/:id/edit',
    title: 'tenants.edit',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/tenants/form/tenant-form').then((m) => m.TenantForm),
  },
  {
    path: 'contracts',
    title: 'contracts.title',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/contracts/list/contracts-list').then((m) => m.ContractsList),
  },
  {
    path: 'contracts/new',
    title: 'contracts.new',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/contracts/form/contract-form').then((m) => m.ContractForm),
  },
  {
    path: 'contracts/:id/edit',
    title: 'contracts.edit',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/contracts/form/contract-form').then((m) => m.ContractForm),
  },
  {
    path: 'payments',
    title: 'payments.title',
    canActivate: [authGuard],
    loadComponent: () => import('./features/payments/list/payments-list').then((m) => m.PaymentsList),
  },
  {
    path: 'payments/new',
    title: 'payments.new',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/payments/form/payment-form').then((m) => m.PaymentForm),
  },
  {
    path: 'maintenance',
    title: 'maintenance.title',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/maintenance/list/maintenance-list').then((m) => m.MaintenanceList),
  },
  {
    path: 'maintenance/new',
    title: 'maintenance.new',
    canActivate: [authGuard, roleGuard('ADMIN', 'TENANT')],
    loadComponent: () =>
      import('./features/maintenance/form/maintenance-form').then((m) => m.MaintenanceForm),
  },
  {
    path: 'maintenance/:id/edit',
    title: 'maintenance.edit',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/maintenance/edit/maintenance-edit').then((m) => m.MaintenanceEdit),
  },
  {
    path: 'not-found',
    title: 'notFound.title',
    loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound),
  },
  { path: '**', redirectTo: 'not-found' },
];
