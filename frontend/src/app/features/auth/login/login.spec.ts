import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [
        Login,
        TranslocoTestingModule.forRoot({
          preloadLangs: true,
          langs: {
            es: {
              auth: {
                email: 'Correo electrónico',
                password: 'Contraseña',
                login: { title: 'Iniciar sesión', submit: 'Entrar', noAccount: '¿No tienes cuenta?' },
              },
              nav: { register: 'Crear cuenta' },
              errors: { INVALID_CREDENTIALS: 'Credenciales incorrectas.', UNKNOWN: 'Error inesperado.' },
              validation: { required: 'Obligatorio.', email: 'Correo no válido.', invalid: 'No válido.' },
            },
            en: {},
          },
          translocoConfig: { availableLangs: ['es', 'en'], defaultLang: 'es', fallbackLang: 'en' },
        }),
      ],
      providers: [provideRouter([{ path: 'home', children: [] }]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('does not call the API when the form is invalid', async () => {
    submit();
    await fixture.whenStable();

    http.expectNone('/api/auth/login');
    expect(fixture.nativeElement.querySelectorAll('.field-error').length).toBe(2);
  });

  it('shows the translated error code when login fails', async () => {
    fill('#email', 'user@test.local');
    fill('#password', 'wrong-password');
    submit();

    http
      .expectOne('/api/auth/login')
      .flush({ code: 'INVALID_CREDENTIALS' }, { status: 401, statusText: 'Unauthorized' });

    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.form-error')?.textContent).toContain(
      'Credenciales incorrectas.',
    );
  });

  function fill(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submit(): void {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
  }
});
