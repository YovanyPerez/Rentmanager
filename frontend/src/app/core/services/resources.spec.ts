import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';
import { DashboardService } from './dashboard.service';
import { MaintenanceService } from './maintenance.service';
import { OwnerService } from './owner.service';
import { PaymentService } from './payment.service';
import { TenantService } from './tenant.service';

interface CallCase {
  name: string;
  method: string;
  url: string;
  call: () => Observable<unknown>;
}

describe('resource services', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const cases: CallCase[] = [
    {
      name: 'OwnerService.list',
      method: 'GET',
      url: '/api/owners',
      call: () => TestBed.inject(OwnerService).list(),
    },
    {
      name: 'OwnerService.create',
      method: 'POST',
      url: '/api/owners',
      call: () =>
        TestBed.inject(OwnerService).create({ fullName: 'A', email: null, phone: null, userId: null }),
    },
    {
      name: 'OwnerService.delete',
      method: 'DELETE',
      url: '/api/owners/3',
      call: () => TestBed.inject(OwnerService).delete(3),
    },
    {
      name: 'TenantService.list',
      method: 'GET',
      url: '/api/tenants',
      call: () => TestBed.inject(TenantService).list(),
    },
    {
      name: 'TenantService.update',
      method: 'PUT',
      url: '/api/tenants/4',
      call: () =>
        TestBed.inject(TenantService).update(4, {
          fullName: 'B',
          email: null,
          phone: null,
          userId: 9,
        }),
    },
    {
      name: 'PaymentService.list',
      method: 'GET',
      url: '/api/payments',
      call: () => TestBed.inject(PaymentService).list(),
    },
    {
      name: 'PaymentService.update',
      method: 'PUT',
      url: '/api/payments/7',
      call: () => TestBed.inject(PaymentService).update(7, { status: 'PAID' }),
    },
    {
      name: 'MaintenanceService.list',
      method: 'GET',
      url: '/api/maintenance',
      call: () => TestBed.inject(MaintenanceService).list(),
    },
    {
      name: 'MaintenanceService.create',
      method: 'POST',
      url: '/api/maintenance',
      call: () =>
        TestBed.inject(MaintenanceService).create({ propertyId: 1, title: 'T', description: null }),
    },
    {
      name: 'MaintenanceService.update',
      method: 'PUT',
      url: '/api/maintenance/8',
      call: () => TestBed.inject(MaintenanceService).update(8, { status: 'IN_PROGRESS' }),
    },
    {
      name: 'DashboardService.statistics',
      method: 'GET',
      url: '/api/dashboard',
      call: () => TestBed.inject(DashboardService).statistics(),
    },
  ];

  for (const testCase of cases) {
    it(`${testCase.name} calls ${testCase.method} ${testCase.url}`, () => {
      testCase.call().subscribe({ next: () => undefined, error: () => undefined });

      const request = http.expectOne({ method: testCase.method, url: testCase.url });
      request.flush(request.request.method === 'DELETE' ? null : {});
    });
  }
});
