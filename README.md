# RentManager

Web-based property rental management platform: properties, owners, tenants, contracts, payments and maintenance requests.

See [`AGENTS.md`](./AGENTS.md) for requirements, architecture and the development plan (current phase: database / backend foundation).

## Repository structure

```text
frontend/   Angular application (Angular 22, Transloco i18n)
backend/    Spring Boot API (Spring Boot 4, Java 17, Maven wrapper)
database/   SQL schema, seed data and database documentation
docs/       Project documentation
```

## Prerequisites

- Node.js 24+ and npm 11+
- Java 17+ (for the backend)
- Docker (for the local MySQL database)

## Getting started

One command (Windows: starts Docker/MySQL, the API and the Angular dev server, then opens the browser):

```powershell
.\dev.cmd      # start everything
.\stop.cmd     # stop everything (database data is kept)
```

Manual start:

1. Start the database (first run applies `database/01_schema.sql` and `database/02_seed.sql`):

```bash
cp .env.example .env   # adjust credentials if needed
docker compose up -d
```

2. Backend (requires the database to be running):

```bash
cd backend
./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
                           # http://localhost:8080/api/health
```

The datasource can be overridden with the `DB_URL`, `DB_USER` and `DB_PASSWORD` environment variables. JWT settings are `JWT_SECRET` and `JWT_TTL_MINUTES`.

`POST /api/auth/register` creates TENANT accounts only; `POST /api/auth/login` returns a JWT. A development ADMIN account is seeded on startup (`admin@rentmanager.local` / `admin1234`) unless `ADMIN_SEED_ENABLED=false`.

3. Frontend:

```bash
cd frontend
npm install
npm start              # http://localhost:4200
```

## Documentation

- `AGENTS.md` — project rules, architecture and phases
- `docs/ARCHITECTURE.md` — backend/data model/API guide (self-contained, for building or replacing the frontend)
- `docs/i18n.md` — internationalisation conventions
- `database/README.md` — data model and database decisions
- `frontend/public/assets/properties/CREDITS.md` — licencias de las fotos de demostración (CC0)
- Manual de usuario — below in this README, also available as `docs/manual-usuario.pdf`

---

# Manual de usuario

RentManager gestiona propiedades, propietarios, inquilinos, contratos, pagos y solicitudes de mantenimiento. La interfaz está disponible en **español** e **inglés** y el idioma se cambia con el selector de la cabecera sin recargar la aplicación.

## Idioma, país y moneda

- El **selector de idioma** cambia todos los textos (es/en).
- El **selector de país** (Colombia 🇨🇴, España 🇪🇸, México 🇲🇽, Estados Unidos 🇺🇸) ajusta el formato de fechas y números y la moneda por defecto de las propiedades nuevas. Ambos selectores se recuerdan entre sesiones.
- Cada propiedad tiene su **propia moneda** (COP, EUR, MXN o USD), que se elige en el formulario y se copia al contrato al crearlo; los pagos heredan la moneda del contrato.
- Los importes se muestran con el símbolo y formato de la región activa (p. ej. `$ 1.800.000` en Colombia, `950 €` en España). El panel suma los ingresos **por moneda**, sin conversión de divisas.

## Cuentas de demostración

Los datos de ejemplo y estas cuentas se crean automáticamente al ejecutar `npm run docs:capture` (desde `frontend/`), que además regenera todas las capturas de este manual.

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | `admin@rentmanager.local` | `admin1234` |
| Propietario | `carlos@docs.local` | `docs12345` |
| Inquilino | `lucia@docs.local` | `docs12345` |

El registro público crea siempre cuentas de **inquilino**; los demás roles se asignan desde el administrador.

## Zona pública

### Página de inicio

Presenta la plataforma, un buscador por ciudad o dirección y las propiedades disponibles destacadas.

![Página de inicio](docs/images/01-inicio.png)

### Buscador de propiedades

Lista las propiedades **disponibles** con búsqueda y filtros. Las propiedades en mantenimiento o inactivas no aparecen en los resultados públicos.

![Buscador de propiedades](docs/images/02-buscar-propiedades.png)

### Detalle de propiedad

Muestra la dirección, la ciudad, la renta mensual y la descripción. Invita a crear una cuenta o iniciar sesión para gestionar el alquiler.

![Detalle de propiedad](docs/images/03-detalle-propiedad.png)

## Registro y acceso

### Iniciar sesión

![Iniciar sesión](docs/images/04-iniciar-sesion.png)

### Crear cuenta

El registro público crea una cuenta de inquilino.

![Registro de cuenta](docs/images/05-registro.png)

### Página no encontrada

Cualquier ruta inexistente redirige a la página 404.

![Página no encontrada](docs/images/06-pagina-no-encontrada.png)

## Rol: Administrador

### Inicio

Cada rol ve un inicio con accesos rápidos a sus secciones.

![Inicio del administrador](docs/images/07-admin-inicio.png)

### Panel

Estadísticas del sistema: total de propiedades, propiedades en alquiler y disponibles, ingresos del mes, gráfico de ingresos de los últimos 12 meses, próximos pagos y solicitudes de mantenimiento abiertas.

![Panel de administración](docs/images/08-admin-panel.png)

### Propiedades

Listado con filtro por estado. Desde cada tarjeta el administrador puede cambiar el estado, editar o desactivar la propiedad.

- `AVAILABLE` → `RENTED` solo se produce al activar un contrato.
- `RENTED` no puede pasar a `MAINTENANCE` ni `INACTIVE` mientras el contrato siga activo.
- `MAINTENANCE` e `INACTIVE` solo pueden volver a `AVAILABLE`.

![Listado de propiedades](docs/images/09-admin-propiedades.png)

Formulario de alta de propiedad, con propietario, dirección, ciudad, descripción, moneda y renta mensual. En la edición se gestiona la galería de fotos: se suben desde el almacenamiento (JPEG, PNG o WebP, hasta 5 MB y máximo 6 por propiedad), se elige la portada, se reordenan y se eliminan. La portada es la foto que aparece en las tarjetas y en el buscador; si no hay fotos, se muestra el marcador con el icono.

![Nueva propiedad](docs/images/10-admin-propiedad-nueva.png)

Edición de una propiedad existente.

![Editar propiedad](docs/images/11-admin-propiedad-editar.png)

### Propietarios

Gestión de propietarios y, opcionalmente, vinculación con una cuenta de usuario con rol propietario.

![Listado de propietarios](docs/images/12-admin-propietarios.png)

![Nuevo propietario](docs/images/13-admin-propietario-nuevo.png)

### Inquilinos

Gestión de inquilinos y vinculación opcional con su cuenta de usuario.

![Listado de inquilinos](docs/images/14-admin-inquilinos.png)

![Nuevo inquilino](docs/images/15-admin-inquilino-nuevo.png)

### Contratos

Listado de contratos con estados `DRAFT`, `ACTIVE`, `EXPIRED` y `TERMINATED`.

- Un contrato en borrador se **activa** cuando la propiedad está disponible y no existe otro contrato activo para ella.
- Al activarlo, la propiedad pasa a `RENTED` y se generan automáticamente las cuotas mensuales.
- Un contrato activo puede **terminarse**; la propiedad vuelve a `AVAILABLE`.
- Los contratos vencidos pasan a `EXPIRED` al consultarse.

![Listado de contratos](docs/images/16-admin-contratos.png)

![Nuevo contrato](docs/images/17-admin-contrato-nuevo.png)

### Pagos

Cuotas generadas por contrato. El administrador puede registrar el pago (marca la cuota como `PAID` con fecha de pago) o cancelarlo.

- `PENDING` → `PAID` / `OVERDUE` / `CANCELLED`
- `OVERDUE` → `PAID` / `CANCELLED`
- Las cuotas vencidas e impagadas se muestran como `OVERDUE` (vencidas).
- `PAID` y `CANCELLED` son estados finales.

![Listado de pagos](docs/images/18-admin-pagos.png)

![Nuevo pago](docs/images/19-admin-pago-nuevo.png)

### Mantenimiento

Solicitudes de mantenimiento asociadas a propiedades. El administrador actualiza el estado y asigna el responsable.

- `OPEN` → `IN_PROGRESS` → `COMPLETED`
- `OPEN` / `IN_PROGRESS` → `CANCELLED`
- `COMPLETED` y `CANCELLED` son estados finales.

![Listado de mantenimiento](docs/images/20-admin-mantenimiento.png)

![Editar solicitud de mantenimiento](docs/images/21-admin-mantenimiento-editar.png)

## Rol: Propietario

Ve únicamente la información de sus propiedades.

### Inicio

![Inicio del propietario](docs/images/22-propietario-inicio.png)

### Propiedades

![Propiedades del propietario](docs/images/23-propietario-propiedades.png)

### Contratos de sus propiedades

![Contratos del propietario](docs/images/24-propietario-contratos.png)

### Pagos de sus contratos

![Pagos del propietario](docs/images/25-propietario-pagos.png)

### Mantenimiento de sus propiedades

![Mantenimiento del propietario](docs/images/26-propietario-mantenimiento.png)

## Rol: Inquilino

Ve únicamente su contrato, sus pagos y sus solicitudes.

### Inicio

![Inicio del inquilino](docs/images/27-inquilino-inicio.png)

### Su contrato

![Contrato del inquilino](docs/images/28-inquilino-contratos.png)

### Sus pagos

![Pagos del inquilino](docs/images/29-inquilino-pagos.png)

### Sus solicitudes de mantenimiento

![Mantenimiento del inquilino](docs/images/30-inquilino-mantenimiento.png)

### Nueva solicitud de mantenimiento

El inquilino solo puede crear solicitudes sobre propiedades que tiene alquiladas actualmente.

![Nueva solicitud de mantenimiento](docs/images/31-inquilino-nueva-incidencia.png)

## Errores y validaciones

La API responde con códigos estables y la interfaz los traduce al idioma activo:

```json
{ "code": "PROPERTY_NOT_FOUND" }
```

```json
{ "code": "VALIDATION_ERROR", "errors": [{ "field": "monthlyRent", "code": "POSITIVE" }] }
```

Ejemplos habituales: credenciales inválidas (`INVALID_CREDENTIALS`), recurso inexistente (`*_NOT_FOUND`), conflicto de contrato activo, fechas de contrato inválidas, transición de estado no permitida y errores de validación de formularios.

## Regenerar el manual

Con el stack en marcha (`.\dev.cmd`):

```bash
cd frontend
npm run docs:capture   # siembra datos demo y regenera docs/images/*.png
npm run docs:pdf       # genera docs/manual-usuario.pdf a partir de este README
```

Los scripts usan Microsoft Edge (o Chrome) instalado en el sistema; se puede indicar otra ruta con la variable de entorno `BROWSER_PATH`. El manual en PDF se genera a partir de este mismo README, de modo que ambos formatos siempre coinciden.
