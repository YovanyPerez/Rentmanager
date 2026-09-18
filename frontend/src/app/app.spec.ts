import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { App } from './app';
import { LanguageService } from './core/i18n/language.service';
import { AppRole } from './shared/models/auth';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [
        App,
        TranslocoTestingModule.forRoot({
          preloadLangs: true,
          langs: {
            es: {
              app: { title: 'RentManager' },
              nav: {
                home: 'Inicio',
                login: 'Iniciar sesión',
                register: 'Crear cuenta',
                logout: 'Cerrar sesión',
                menu: 'Abrir menú',
                primary: 'Navegación principal',
              },
              dashboard: { title: 'Panel' },
              properties: { title: 'Propiedades' },
              owners: { title: 'Propietarios' },
              tenants: { title: 'Inquilinos' },
              contracts: { title: 'Contratos' },
              payments: { title: 'Pagos' },
              maintenance: { title: 'Mantenimiento' },
              enums: { role: { ADMIN: 'Administrador', OWNER: 'Propietario', TENANT: 'Inquilino' } },
              langs: { es: 'Español 🇪🇸', en: 'English 🇺🇸' },
            },
            en: {
              app: { title: 'RentManager' },
              nav: { logout: 'Sign out', menu: 'Open menu', primary: 'Main navigation' },
              enums: { role: { ADMIN: 'Administrator' } },
              langs: { es: 'Español 🇪🇸', en: 'English 🇺🇸' },
            },
          },
          translocoConfig: {
            availableLangs: ['es', 'en'],
            defaultLang: 'es',
            fallbackLang: 'en',
            reRenderOnLangChange: true,
          },
        }),
      ],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the shell brand when there is a session', async () => {
    loginAs('ADMIN');
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.sidebar__brand')?.textContent).toContain('RentManager');
    expect(compiled.querySelectorAll('.sidebar__link').length).toBeGreaterThan(0);
  });

  it('should switch language at runtime and persist it', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const trigger = fixture.nativeElement.querySelector('.lang__trigger') as HTMLButtonElement;
    expect(trigger).toBeTruthy();
    trigger.click();
    await fixture.whenStable();

    const options = fixture.nativeElement.querySelectorAll('.lang__option');
    expect(options.length).toBe(2);

    options[1].click();

    expect(TestBed.inject(LanguageService).lang()).toBe('en');
    expect(localStorage.getItem('rentmanager.lang')).toBe('en');
  });

  function loginAs(role: AppRole): void {
    localStorage.setItem('rentmanager.token', 'jwt-token');
    localStorage.setItem(
      'rentmanager.auth',
      JSON.stringify({
        userId: 1,
        email: 'user@test.local',
        fullName: 'Test User',
        role,
        expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      }),
    );
  }
});
