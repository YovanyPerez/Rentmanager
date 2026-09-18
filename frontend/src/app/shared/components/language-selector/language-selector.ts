import { Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';

@Component({
  selector: 'app-language-selector',
  imports: [TranslocoPipe],
  template: `
    <div class="language-selector">
      @for (lang of i18n.supportedLangs; track lang) {
        <button
          type="button"
          [disabled]="i18n.lang() === lang"
          (click)="i18n.use(lang)"
        >
          {{ 'langs.' + lang | transloco }}
        </button>
      }
    </div>
  `,
})
export class LanguageSelector {
  protected readonly i18n = inject(LanguageService);
}
