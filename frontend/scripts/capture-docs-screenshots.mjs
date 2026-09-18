// Capturas para el manual del README. Requiere el stack levantado (dev.cmd).
// Uso: npm run docs:capture   (desde frontend/)
// Salida: docs/images/*.png en la raíz del repositorio.
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { launchBrowser } from './docs-browser.mjs';

const WEB = process.env.DOCS_WEB_URL ?? 'http://localhost:4200';
const API = `${WEB}/api`;
const ROOT = resolve(import.meta.dirname, '..', '..');
const OUT = resolve(ROOT, 'docs', 'images');

const ADMIN = {
  email: process.env.ADMIN_EMAIL ?? 'admin@rentmanager.local',
  password: process.env.ADMIN_PASSWORD ?? 'admin1234',
};
const DEMO_PASSWORD = 'docs12345';
const OWNER_USER = {
  email: 'carlos@docs.local',
  password: DEMO_PASSWORD,
  fullName: 'Carlos Propietario',
};
const TENANT_USER = {
  email: 'lucia@docs.local',
  password: DEMO_PASSWORD,
  fullName: 'Lucía Inquilina',
};

async function api(path, { method = 'GET', token, body } = {}) {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const error = new Error(`${method} ${path} -> ${res.status} ${JSON.stringify(data)}`);
    error.status = res.status;
    throw error;
  }
  return data;
}

const login = (email, password) => api('/auth/login', { method: 'POST', body: { email, password } });

async function ensureUser(user) {
  try {
    return await api('/auth/register', { method: 'POST', body: user });
  } catch (error) {
    if (error.status !== 409) throw error;
    return login(user.email, user.password);
  }
}

function readEnvFile() {
  const file = resolve(ROOT, '.env');
  if (!existsSync(file)) return {};
  return Object.fromEntries(
    readFileSync(file, 'utf8')
      .split(/\r?\n/)
      .filter((line) => line.includes('=') && !line.trim().startsWith('#'))
      .map((line) => {
        const index = line.indexOf('=');
        return [line.slice(0, index).trim(), line.slice(index + 1).trim()];
      }),
  );
}

function promoteToOwner(email) {
  const env = readEnvFile();
  const password = env.MYSQL_ROOT_PASSWORD;
  const database = env.MYSQL_DATABASE ?? 'rentmanager';
  if (!password) {
    throw new Error(
      `No hay MYSQL_ROOT_PASSWORD en .env. Ejecuta manualmente:\n` +
        `  UPDATE users SET role='OWNER' WHERE email='${email}';`,
    );
  }
  // ponytail: -p en argv solo vale para la BD local de desarrollo.
  execFileSync(
    'docker',
    ['exec', 'rentmanager-mysql', 'mysql', '-uroot', `-p${password}`, database, '-e',
      `UPDATE users SET role='OWNER' WHERE email='${email}'`],
    { stdio: 'pipe' },
  );
}

async function ensureOwner(adminToken, user, userId) {
  const owners = await api('/owners', { token: adminToken });
  const found = owners.find((owner) => owner.email === user.email);
  if (found) return found;
  return api('/owners', {
    method: 'POST',
    token: adminToken,
    body: { fullName: user.fullName, email: user.email, phone: '+57 300 555 1111', userId },
  });
}

async function ensureTenant(adminToken, user, userId) {
  const tenants = await api('/tenants', { token: adminToken });
  const found = tenants.find((tenant) => tenant.email === user.email);
  if (found) return found;
  return api('/tenants', {
    method: 'POST',
    token: adminToken,
    body: { fullName: user.fullName, email: user.email, phone: '+57 300 555 2222', userId },
  });
}

async function ensureProperty(adminToken, ownerId, data) {
  const properties = await api('/properties', { token: adminToken });
  const found = properties.find((property) => property.address === data.address);
  if (found) return found;
  return api('/properties', { method: 'POST', token: adminToken, body: { ownerId, ...data } });
}

async function uploadImage(token, property, fileName) {
  const data = readFileSync(resolve(ROOT, 'frontend', 'public', 'assets', 'properties', fileName));
  const form = new FormData();
  form.append('file', new Blob([data], { type: 'image/jpeg' }), fileName);
  const res = await fetch(`${API}/properties/${property.id}/images`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  if (!res.ok) {
    throw new Error(`POST /properties/${property.id}/images -> ${res.status} ${await res.text()}`);
  }
}

async function ensurePhoto(adminToken, property) {
  const fileName = DEMO_PHOTOS[property.address];
  if (!fileName || (property.images?.length ?? 0) > 0) return;
  await uploadImage(adminToken, property, fileName);
}

const DEMO_PHOTOS = {
  'Carrera 7 # 45-12, Apto 302': 'cocina-madrid.jpg',
  'Calle 10 # 40-20, Apto 501': 'apartamento-berlin.jpg',
  'Carrera 15 # 85-30, Apto 501': 'piso-madrid.jpg',
  'Calle 5 # 23-45, Apto 1201': 'piso-valencia.jpg',
  'Calle Betis 12, 3ºA': 'casa-sevilla.jpg',
};

async function ensureContract(adminToken, request) {
  const contracts = await api('/contracts', { token: adminToken });
  return contracts.find(
    (contract) =>
      contract.propertyId === request.propertyId && contract.tenantId === request.tenantId,
  );
}

const iso = (date) => date.toISOString().slice(0, 10);
const monthsFromToday = (months) => {
  const date = new Date();
  date.setMonth(date.getMonth() + months);
  return date;
};

async function seedDemoData() {
  const admin = await login(ADMIN.email, ADMIN.password);
  const adminToken = admin.token;

  const ownerSignup = await ensureUser(OWNER_USER);
  promoteToOwner(OWNER_USER.email);
  const owner = await login(OWNER_USER.email, OWNER_USER.password);

  const tenantSignup = await ensureUser(TENANT_USER);
  const tenant = await login(TENANT_USER.email, TENANT_USER.password);

  const ownerEntity = await ensureOwner(adminToken, OWNER_USER, ownerSignup.userId ?? owner.userId);
  const tenantEntity = await ensureTenant(adminToken, TENANT_USER, tenantSignup.userId ?? tenant.userId);

  const available = await ensureProperty(adminToken, ownerEntity.id, {
    address: 'Carrera 15 # 85-30, Apto 501',
    city: 'Bogotá',
    description: 'Apartamento moderno cerca del Parque de la 93.',
    currency: 'COP',
    monthlyRent: 2200000,
  });
  const rented = await ensureProperty(adminToken, ownerEntity.id, {
    address: 'Calle 5 # 23-45, Apto 1201',
    city: 'Medellín',
    description: 'Apartamento con vista a las montañas en El Poblado.',
    currency: 'COP',
    monthlyRent: 1600000,
  });
  await ensureProperty(adminToken, ownerEntity.id, {
    address: 'Calle Betis 12, 3ºA',
    city: 'Sevilla',
    description: 'Piso reformado junto al río, con ascensor.',
    currency: 'EUR',
    monthlyRent: 950,
  });

  const demoProperties = await api('/properties', { token: adminToken });
  for (const property of demoProperties) {
    await ensurePhoto(adminToken, property);
  }

  let activeContract = await ensureContract(adminToken, {
    propertyId: rented.id,
    tenantId: tenantEntity.id,
    startDate: iso(monthsFromToday(-6)),
    endDate: iso(monthsFromToday(6)),
    monthlyRent: rented.monthlyRent,
  });
  if (!activeContract) {
    const draft = await api('/contracts', {
      method: 'POST',
      token: adminToken,
      body: {
        propertyId: rented.id,
        tenantId: tenantEntity.id,
        startDate: iso(monthsFromToday(-6)),
        endDate: iso(monthsFromToday(6)),
        monthlyRent: rented.monthlyRent,
      },
    });
    activeContract = await api(`/contracts/${draft.id}/activate`, { method: 'POST', token: adminToken });
  }

  const draftRequest = {
    propertyId: available.id,
    tenantId: tenantEntity.id,
    startDate: iso(monthsFromToday(1)),
    endDate: iso(monthsFromToday(13)),
    monthlyRent: available.monthlyRent,
  };
  if (!(await ensureContract(adminToken, draftRequest))) {
    await api('/contracts', { method: 'POST', token: adminToken, body: draftRequest });
  }

  const payments = await api('/payments', { token: adminToken });
  const installments = payments
    .filter((payment) => payment.contractId === activeContract.id)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  let paidCount = installments.filter((payment) => payment.status === 'PAID').length;
  for (const payment of installments) {
    if (paidCount >= 2) break;
    if (payment.status === 'PENDING' || payment.status === 'OVERDUE') {
      await api(`/payments/${payment.id}`, {
        method: 'PUT',
        token: adminToken,
        body: { status: 'PAID', paidDate: payment.dueDate },
      });
      paidCount += 1;
    }
  }

  const maintenanceTitles = [
    'Fuga de agua en el baño',
    'Caldera sin presión',
    'Persiana rota en el salón',
    'Ruido en el ascensor por las noches',
  ];
  const existing = await api('/maintenance', { token: adminToken });
  const created = [];
  for (const title of maintenanceTitles) {
    if (existing.some((request) => request.title === title)) continue;
    created.push(
      await api('/maintenance', {
        method: 'POST',
        token: tenant.token,
        body: {
          propertyId: rented.id,
          title,
          description: title === maintenanceTitles[0]
            ? 'El grifo del lavabo pierde agua desde hace dos días.'
            : 'Solicito revisión cuando sea posible.',
        },
      }),
    );
  }
  if (created[0]) {
    await api(`/maintenance/${created[0].id}`, {
      method: 'PUT', token: adminToken, body: { status: 'IN_PROGRESS', assignedTo: 'Fontanería Gómez' },
    });
    await api(`/maintenance/${created[0].id}`, {
      method: 'PUT', token: adminToken, body: { status: 'COMPLETED', assignedTo: 'Fontanería Gómez' },
    });
  }
  if (created[1]) {
    await api(`/maintenance/${created[1].id}`, {
      method: 'PUT', token: adminToken, body: { status: 'IN_PROGRESS', assignedTo: 'ClimaServ S.L.' },
    });
  }
  if (created[3]) {
    await api(`/maintenance/${created[3].id}`, {
      method: 'PUT', token: adminToken, body: { status: 'CANCELLED', assignedTo: null },
    });
  }

  const all = await api('/maintenance', { token: adminToken });
  const inProgress =
    all.find((request) => request.status === 'IN_PROGRESS') ??
    all.find((request) => request.status === 'OPEN') ??
    all[0];

  return { admin, owner, tenant, available, maintenanceId: inProgress?.id };
}

async function main() {
  for (const [label, url] of [
    ['API', 'http://localhost:8080/api/health'],
    ['web', WEB],
  ]) {
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(String(res.status));
    } catch {
      throw new Error(`El stack no está arrancado (${label}: ${url}). Ejecuta .\\dev.cmd y reintenta.`);
    }
  }

  mkdirSync(OUT, { recursive: true });

  console.log('Sembrando datos demo...');
  const seed = await seedDemoData();
  console.log(`  datos listos (propiedad pública #${seed.available.id})`);

  const browser = await launchBrowser();

  async function newSession(auth) {
    const context = await browser.createBrowserContext();
    const page = await context.newPage();
    await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 1 });
    await page.evaluateOnNewDocument(
      (authData) => {
        localStorage.setItem('rentmanager.lang', 'es');
        if (authData) {
          localStorage.setItem('rentmanager.token', authData.token);
          localStorage.setItem(
            'rentmanager.auth',
            JSON.stringify({
              userId: authData.userId,
              email: authData.email,
              fullName: authData.fullName,
              role: authData.role,
              expiresAt: authData.expiresAt,
            }),
          );
        }
      },
      auth ?? null,
    );
    return page;
  }

  async function shot(page, file, url) {
    await page.goto(`${WEB}${url}`, { waitUntil: 'domcontentloaded', timeout: 60000 });
    await page
      .waitForFunction(() => !document.querySelector('app-skeleton'), { timeout: 20000 })
      .catch(() => {});
    await new Promise((done) => setTimeout(done, 900));
    await page.screenshot({ path: resolve(OUT, file) });
    console.log(`  ${file}`);
  }

  const publicPage = await newSession(null);
  await shot(publicPage, '01-inicio.png', '/');
  await shot(publicPage, '02-buscar-propiedades.png', '/search');
  await shot(publicPage, '03-detalle-propiedad.png', `/property/${seed.available.id}`);
  await shot(publicPage, '04-iniciar-sesion.png', '/login');
  await shot(publicPage, '05-registro.png', '/register');
  await shot(publicPage, '06-pagina-no-encontrada.png', '/not-found');

  const adminPage = await newSession(seed.admin);
  await shot(adminPage, '07-admin-inicio.png', '/home');
  await shot(adminPage, '08-admin-panel.png', '/dashboard');
  await shot(adminPage, '09-admin-propiedades.png', '/properties');
  await shot(adminPage, '10-admin-propiedad-nueva.png', '/properties/new');
  await shot(adminPage, '11-admin-propiedad-editar.png', `/properties/${seed.available.id}/edit`);
  await shot(adminPage, '12-admin-propietarios.png', '/owners');
  await shot(adminPage, '13-admin-propietario-nuevo.png', '/owners/new');
  await shot(adminPage, '14-admin-inquilinos.png', '/tenants');
  await shot(adminPage, '15-admin-inquilino-nuevo.png', '/tenants/new');
  await shot(adminPage, '16-admin-contratos.png', '/contracts');
  await shot(adminPage, '17-admin-contrato-nuevo.png', '/contracts/new');
  await shot(adminPage, '18-admin-pagos.png', '/payments');
  await shot(adminPage, '19-admin-pago-nuevo.png', '/payments/new');
  await shot(adminPage, '20-admin-mantenimiento.png', '/maintenance');
  if (seed.maintenanceId) {
    await shot(adminPage, '21-admin-mantenimiento-editar.png', `/maintenance/${seed.maintenanceId}/edit`);
  }

  const ownerPage = await newSession(seed.owner);
  await shot(ownerPage, '22-propietario-inicio.png', '/home');
  await shot(ownerPage, '23-propietario-propiedades.png', '/properties');
  await shot(ownerPage, '24-propietario-contratos.png', '/contracts');
  await shot(ownerPage, '25-propietario-pagos.png', '/payments');
  await shot(ownerPage, '26-propietario-mantenimiento.png', '/maintenance');

  const tenantPage = await newSession(seed.tenant);
  await shot(tenantPage, '27-inquilino-inicio.png', '/home');
  await shot(tenantPage, '28-inquilino-contratos.png', '/contracts');
  await shot(tenantPage, '29-inquilino-pagos.png', '/payments');
  await shot(tenantPage, '30-inquilino-mantenimiento.png', '/maintenance');
  await shot(tenantPage, '31-inquilino-nueva-incidencia.png', '/maintenance/new');

  await browser.close();
  console.log(`\nCapturas guardadas en ${OUT}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
