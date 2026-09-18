import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ContractService } from './contract.service';

describe('ContractService', () => {
  let service: ContractService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ContractService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should activate a contract with POST', () => {
    service.activate(5).subscribe();

    const request = http.expectOne({ method: 'POST', url: '/api/contracts/5/activate' });
    expect(request.request.body).toEqual({});
    request.flush({});
  });

  it('should terminate a contract with POST', () => {
    service.terminate(5).subscribe();

    http.expectOne({ method: 'POST', url: '/api/contracts/5/terminate' }).flush({});
  });

  it('should create a contract with POST', () => {
    const body = {
      propertyId: 1,
      tenantId: 2,
      startDate: '2030-01-01',
      endDate: '2030-12-31',
      monthlyRent: 1000,
    };
    service.create(body).subscribe();

    const request = http.expectOne({ method: 'POST', url: '/api/contracts' });
    expect(request.request.body).toEqual(body);
    request.flush({});
  });
});
