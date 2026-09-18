import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PropertyService } from './property.service';

describe('PropertyService', () => {
  let service: PropertyService;
  let http: HttpTestingController;

  const property = {
    id: 1,
    ownerId: 2,
    ownerName: 'Ana',
    address: 'Calle Mayor 1',
    city: 'Madrid',
    description: null,
    imageUrl: null,
    monthlyRent: 1200,
    status: 'AVAILABLE' as const,
    createdAt: '2030-01-01T00:00:00Z',
    updatedAt: '2030-01-01T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PropertyService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should list properties', () => {
    let result = 0;
    service.list().subscribe((properties) => (result = properties.length));

    http.expectOne('/api/properties').flush([property, { ...property, id: 2 }]);

    expect(result).toBe(2);
  });

  it('should create a property', () => {
    service
      .create({ ownerId: 2, address: 'A', city: 'B', description: null, imageUrl: null, monthlyRent: 100 })
      .subscribe((created) => expect(created.id).toBe(1));

    const request = http.expectOne({ method: 'POST', url: '/api/properties' });
    expect(request.request.body).toEqual({
      ownerId: 2,
      address: 'A',
      city: 'B',
      description: null,
      imageUrl: null,
      monthlyRent: 100,
    });
    request.flush(property);
  });

  it('should change the status with PATCH', () => {
    service.changeStatus(1, 'MAINTENANCE').subscribe();

    const request = http.expectOne({ method: 'PATCH', url: '/api/properties/1/status' });
    expect(request.request.body).toEqual({ status: 'MAINTENANCE' });
    request.flush({ ...property, status: 'MAINTENANCE' });
  });

  it('should deactivate with DELETE', () => {
    service.deactivate(1).subscribe();

    http.expectOne({ method: 'DELETE', url: '/api/properties/1' }).flush(null, {
      status: 204,
      statusText: 'No Content',
    });
  });
});
