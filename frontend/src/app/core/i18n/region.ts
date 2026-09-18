import { CurrencyCode } from '../../shared/models/currency';

export type AppRegion = 'CO' | 'ES' | 'MX' | 'US';

export interface AppRegionConfig {
  id: AppRegion;
  currency: CurrencyCode;
  /** Locale used for dates and numbers, per translation language. */
  locales: { es: string; en: string };
}

export const REGIONS: readonly AppRegionConfig[] = [
  { id: 'CO', currency: 'COP', locales: { es: 'es-CO', en: 'en-US' } },
  { id: 'ES', currency: 'EUR', locales: { es: 'es', en: 'en-US' } },
  { id: 'MX', currency: 'MXN', locales: { es: 'es-MX', en: 'en-US' } },
  { id: 'US', currency: 'USD', locales: { es: 'es-US', en: 'en-US' } },
];

export const DEFAULT_REGION: AppRegion = 'CO';

const STORAGE_KEY = 'rentmanager.region';

export function regionConfig(region: AppRegion): AppRegionConfig {
  return REGIONS.find((config) => config.id === region) ?? REGIONS[0];
}

export function resolveInitialRegion(): AppRegion {
  const saved = localStorage.getItem(STORAGE_KEY);
  return isAppRegion(saved) ? saved : DEFAULT_REGION;
}

export function persistRegion(region: AppRegion): void {
  localStorage.setItem(STORAGE_KEY, region);
}

function isAppRegion(value: string | null): value is AppRegion {
  return value !== null && (REGIONS as readonly { id: string }[]).some((region) => region.id === value);
}
