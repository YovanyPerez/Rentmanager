import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localeEn from '@angular/common/locales/en';
import localeEs from '@angular/common/locales/es';
import localeEsCo from '@angular/common/locales/es-CO';
import localeEsMx from '@angular/common/locales/es-MX';
import localeEsUs from '@angular/common/locales/es-US';
import {
  ApplicationConfig,
  inject,
  isDevMode,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withViewTransitions, TitleStrategy } from '@angular/router';
import { TranslocoService, provideTransloco } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { routes } from './app.routes';
import { FALLBACK_LANG, SUPPORTED_LANGS, resolveInitialLang } from './core/i18n/language';
import { TranslatedTitleStrategy } from './core/i18n/translated-title.strategy';
import { TranslocoHttpLoader } from './core/i18n/transloco-loader';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { RobotsService } from './core/seo/robots.service';

registerLocaleData(localeEs);
registerLocaleData(localeEn);
registerLocaleData(localeEsCo);
registerLocaleData(localeEsMx);
registerLocaleData(localeEsUs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withViewTransitions()),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideTransloco({
      config: {
        availableLangs: [...SUPPORTED_LANGS],
        defaultLang: resolveInitialLang(),
        fallbackLang: FALLBACK_LANG,
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
    { provide: TitleStrategy, useClass: TranslatedTitleStrategy },
    provideAppInitializer(() => {
      const transloco = inject(TranslocoService);
      return firstValueFrom(transloco.load(transloco.getActiveLang()));
    }),
    provideAppInitializer(() => {
      inject(RobotsService);
    }),
  ],
};
