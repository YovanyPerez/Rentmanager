import { Component, HostListener, inject, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { RegionService } from '../../../core/i18n/region.service';
import { AppRegion } from '../../../core/i18n/region';
import { Icon } from '../icon/icon';

@Component({
  selector: 'app-region-selector',
  imports: [TranslocoPipe, Icon],
  template: `
    <div class="region">
      <button
        type="button"
        class="region__trigger"
        [attr.aria-label]="'nav.region' | transloco"
        aria-haspopup="listbox"
        [attr.aria-expanded]="open()"
        (click)="toggle($event)"
      >
        <span class="region__code">{{ region.region() }}</span>
        <app-icon name="chevron-down" [size]="14" />
      </button>

      @if (open()) {
        <ul class="region__menu" role="listbox" animate.enter="enter-animation" (click)="$event.stopPropagation()">
          @for (item of region.supportedRegions; track item.id) {
            <li role="presentation">
              <button
                type="button"
                class="region__option"
                role="option"
                [attr.aria-selected]="region.region() === item.id"
                (click)="select(item.id)"
              >
                <span>{{ 'regions.' + item.id | transloco }}</span>
                @if (region.region() === item.id) {
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
    .region {
      position: relative;
    }

    .region__trigger {
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

    .region__trigger:hover {
      border-color: var(--color-primary);
      color: var(--color-primary);
    }

    .region__code {
      letter-spacing: 0.04em;
    }

    .region__menu {
      position: absolute;
      top: calc(100% + 6px);
      right: 0;
      z-index: 30;
      min-width: 200px;
      margin: 0;
      padding: 6px;
      list-style: none;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      background: var(--color-surface);
      box-shadow: var(--shadow-md);
    }

    .region__option {
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

    .region__option:hover {
      background: var(--color-primary-soft);
    }

    .region__option[aria-selected='true'] {
      color: var(--color-primary);
      font-weight: 600;
    }
  `,
})
export class RegionSelector {
  protected readonly region = inject(RegionService);
  protected readonly open = signal(false);

  protected toggle(event: MouseEvent): void {
    event.stopPropagation();
    this.open.update((open) => !open);
  }

  protected select(region: AppRegion): void {
    this.region.use(region);
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
