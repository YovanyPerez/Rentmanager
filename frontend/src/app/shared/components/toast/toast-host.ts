import { Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { Icon } from '../icon/icon';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-host',
  imports: [TranslocoPipe, Icon],
  template: `
    <div class="toasts" aria-live="polite">
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          class="toast"
          [class.toast--success]="toast.kind === 'success'"
          [class.toast--error]="toast.kind === 'error'"
          animate.enter="enter-animation"
          animate.leave="leave-animation"
        >
          <app-icon [name]="toast.kind === 'success' ? 'check' : 'alert'" [size]="16" />
          <span>{{ toast.key | transloco }}</span>
          <button
            type="button"
            class="toast__close"
            [attr.aria-label]="'common.close' | transloco"
            (click)="toastService.dismiss(toast.id)"
          >
            <app-icon name="x" [size]="14" />
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .toasts {
      position: fixed;
      right: 16px;
      bottom: 16px;
      z-index: 50;
      display: grid;
      gap: 8px;
      max-width: 360px;
    }

    .toast {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 12px;
      border: 1px solid var(--color-border);
      border-left: 4px solid var(--color-neutral);
      border-radius: var(--radius-sm);
      background: var(--color-surface);
      box-shadow: var(--shadow-md);
      font-size: var(--text-sm);
    }

    .toast--success {
      border-left-color: var(--color-success);
    }

    .toast--error {
      border-left-color: var(--color-danger);
    }

    .toast__close {
      margin-left: auto;
      display: inline-flex;
      padding: 4px;
      border: none;
      border-radius: var(--radius-sm);
      background: transparent;
      color: var(--color-text-muted);
      cursor: pointer;
    }

    .toast__close:hover {
      background: var(--color-neutral-soft);
    }

    @media (max-width: 767px) {
      .toasts {
        left: 16px;
        right: 16px;
        bottom: 16px;
        max-width: none;
      }
    }
  `,
})
export class ToastHost {
  protected readonly toastService = inject(ToastService);
}
