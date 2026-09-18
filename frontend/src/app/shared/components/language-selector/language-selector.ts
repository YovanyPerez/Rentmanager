import { Component, HostListener, inject, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppLang } from '../../../core/i18n/language';
import { LanguageService } from '../../../core/i18n/language.service';
import { Icon } from '../icon/icon';

@Component({
  selector: 'app-language-selector',
  imports: [TranslocoPipe, Icon],
  template: `
    <div class="lang">
      <button
        type="button"
        class="lang__trigger"
        [attr.aria-label]="'nav.language' | transloco"
        aria-haspopup="listbox"
        [attr.aria-expanded]="open()"
        (click)="toggle($event)"
      >
        <span class="lang__code">{{ i18n.lang().toUpperCase() }}</span>
        <app-icon name="chevron-down" [size]="14" />
      </button>

      @if (open()) {
        <ul class="lang__menu" role="listbox" animate.enter="enter-animation" (click)="$event.stopPropagation()">
          @for (lang of i18n.supportedLangs; track lang) {
            <li role="presentation">
              <button
                type="button"
                class="lang__option"
                role="option"
                [attr.aria-selected]="i18n.lang() === lang"
                (click)="select(lang)"
              >
                <span>{{ 'langs.' + lang | transloco }}</span>
                @if (i18n.lang() === lang) {
                  <app-icon name="check" [size]="14" />
                }
              </button>
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: `
    .lang {
      position: relative;
    }

    .lang__trigger {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 34px;
      padding: 0 10px;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      background: var(--color-surface);
      color: var(--color-text);
      font: inherit;
      font-size: var(--text-sm);
      font-weight: 600;
      cursor: pointer;
      transition:
        border-color var(--duration-fast) var(--ease-standard),
        color var(--duration-fast) var(--ease-standard),
        background-color var(--duration-fast) var(--ease-standard);
    }

    .lang__trigger:hover {
      border-color: var(--color-primary);
      color: var(--color-primary);
    }

    .lang__code {
      letter-spacing: 0.04em;
    }

    .lang__menu {
      position: absolute;
      top: calc(100% + 6px);
      right: 0;
      z-index: 30;
      min-width: 176px;
      margin: 0;
      padding: 6px;
      list-style: none;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      background: var(--color-surface);
      box-shadow: var(--shadow-md);
    }

    .lang__option {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      width: 100%;
      padding: 8px 10px;
      border: none;
      border-radius: var(--radius-sm);
      background: transparent;
      color: var(--color-text);
      font: inherit;
      font-size: var(--text-sm);
      text-align: left;
      cursor: pointer;
      transition: background-color var(--duration-fast) var(--ease-standard);
    }

    .lang__option:hover {
      background: var(--color-primary-soft);
    }

    .lang__option[aria-selected='true'] {
      color: var(--color-primary);
      font-weight: 600;
    }
  `,
})
export class LanguageSelector {
  protected readonly i18n = inject(LanguageService);
  protected readonly open = signal(false);

  protected toggle(event: MouseEvent): void {
    event.stopPropagation();
    this.open.update((open) => !open);
  }

  protected select(lang: AppLang): void {
    this.i18n.use(lang);
    this.open.set(false);
  }

  @HostListener('document:click')
  protected close(): void {
    this.open.set(false);
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.open.set(false);
  }
}
