export type AppLang = 'es' | 'en';

export const SUPPORTED_LANGS: readonly AppLang[] = ['es', 'en'];

export const DEFAULT_LANG: AppLang = 'es';
export const FALLBACK_LANG: AppLang = 'en';

const STORAGE_KEY = 'rentmanager.lang';

export function resolveInitialLang(): AppLang {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (isAppLang(saved)) {
    return saved;
  }
  const browser = navigator.language.slice(0, 2).toLowerCase();
  return isAppLang(browser) ? browser : DEFAULT_LANG;
}

export function persistLang(lang: AppLang): void {
  localStorage.setItem(STORAGE_KEY, lang);
}

function isAppLang(value: string | null): value is AppLang {
  return value !== null && (SUPPORTED_LANGS as readonly string[]).includes(value);
}
