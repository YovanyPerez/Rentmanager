import { Injectable, Signal, effect, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { AppLang, SUPPORTED_LANGS, persistLang } from './language';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly transloco = inject(TranslocoService);

  /** Active language code, reactive. */
  readonly lang: Signal<string> = this.transloco.activeLang;

  readonly supportedLangs = SUPPORTED_LANGS;

  constructor() {
    effect(() => {
      document.documentElement.lang = this.lang();
    });
  }

  /** Switches the active language and persists the preference. */
  use(lang: AppLang): void {
    this.transloco.setActiveLang(lang);
    persistLang(lang);
  }

  /** Imperative translation for text produced outside templates. */
  t(key: string, params?: Record<string, unknown>): string {
    return this.transloco.translate(key, params);
  }
}
