import { Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { Icon, IconName } from '../icon/icon';

@Component({
  selector: 'app-empty-state',
  imports: [Icon, TranslocoPipe],
  template: `
    <div class="empty">
      <app-icon [name]="icon()" [size]="28" />
      <p class="empty__title">{{ titleKey() | transloco }}</p>
      @if (hintKey(); as hint) {
        <p class="empty__hint">{{ hint | transloco }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: `
    .empty {
      display: grid;
      justify-items: center;
      gap: 8px;
      padding: 48px 24px;
      text-align: center;
      color: var(--color-text-muted);
      border: 1px dashed var(--color-border);
      border-radius: var(--radius-md);
      background: var(--color-surface);
    }

    .empty__title {
      margin: 4px 0 0;
      font-size: var(--text-md);
      font-weight: 500;
      color: var(--color-text);
    }

    .empty__hint {
      margin: 0;
      font-size: var(--text-sm);
    }
  `,
})
export class EmptyState {
  readonly icon = input<IconName>('inbox');
  readonly titleKey = input.required<string>();
  readonly hintKey = input<string | null>(null);
}
