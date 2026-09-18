import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { StatusBadge } from './status-badge';

describe('StatusBadge', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        StatusBadge,
        TranslocoTestingModule.forRoot({
          preloadLangs: true,
          langs: {
            es: {
              enums: {
                propertyStatus: { AVAILABLE: 'Disponible', RENTED: 'Alquilada' },
                paymentStatus: { OVERDUE: 'Vencido' },
                maintenanceStatus: { OPEN: 'Abierta' },
                contractStatus: { ACTIVE: 'Activo' },
              },
            },
            en: {},
          },
          translocoConfig: { availableLangs: ['es', 'en'], defaultLang: 'es', fallbackLang: 'en' },
        }),
      ],
    }).compileComponents();
  });

  it('translates the status and applies the variant class', async () => {
    const fixture = TestBed.createComponent(StatusBadge);
    fixture.componentRef.setInput('kind', 'property');
    fixture.componentRef.setInput('status', 'AVAILABLE');
    await fixture.whenStable();

    const chip = fixture.nativeElement.querySelector('span') as HTMLElement;
    expect(chip.textContent).toContain('Disponible');
    expect(chip.classList.contains('chip--success')).toBe(true);
  });

  it('maps overdue payments to danger and unknown statuses to neutral', async () => {
    const fixture = TestBed.createComponent(StatusBadge);
    fixture.componentRef.setInput('kind', 'payment');
    fixture.componentRef.setInput('status', 'OVERDUE');
    await fixture.whenStable();
    expect(
      (fixture.nativeElement.querySelector('span') as HTMLElement).classList.contains('chip--danger'),
    ).toBe(true);

    fixture.componentRef.setInput('kind', 'payment');
    fixture.componentRef.setInput('status', 'WHATEVER');
    await fixture.whenStable();
    expect(
      (fixture.nativeElement.querySelector('span') as HTMLElement).classList.contains(
        'chip--neutral',
      ),
    ).toBe(true);
  });
});
