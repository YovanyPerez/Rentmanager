import { Injectable, Signal, computed, inject, signal } from '@angular/core';
import { LanguageService } from './language.service';
import { AppRegion, REGIONS, regionConfig, persistRegion, resolveInitialRegion } from './region';
import { CurrencyCode } from '../../shared/models/currency';

@Injectable({ providedIn: 'root' })
export class RegionService {
  private readonly i18n = inject(LanguageService);
  private readonly current = signal<AppRegion>(resolveInitialRegion());

  readonly region = this.current.asReadonly();
  readonly supportedRegions = REGIONS;

  /** Default currency for new properties in the active region. */
  readonly currency: Signal<CurrencyCode> = computed(() => regionConfig(this.current()).currency);

  /** Locale for dates and numbers, combining the language and the region. */
  readonly locale: Signal<string> = computed(() => {
    const config = regionConfig(this.current());
    return this.i18n.lang() === 'en' ? config.locales.en : config.locales.es;
  });

  use(region: AppRegion): void {
    this.current.set(region);
    persistRegion(region);
  }
}
