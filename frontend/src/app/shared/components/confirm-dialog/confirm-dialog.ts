import { Component, ElementRef, HostListener, input, output, viewChild } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-confirm-dialog',
  imports: [TranslocoPipe],
  template: `
    @if (open()) {
      <div class="overlay" animate.enter="enter-animation" (click)="cancelled.emit()">
        <div
          #dialog
          class="dialog"
          role="dialog"
          aria-modal="true"
          tabindex="-1"
          [attr.aria-label]="'common.confirmTitle' | transloco"
          (click)="$event.stopPropagation()"
        >
          <h2>{{ 'common.confirmTitle' | transloco }}</h2>
          <p class="dialog__message">{{ messageKey() | transloco }}</p>
          <div class="dialog__actions">
            <button type="button" class="btn btn--secondary" (click)="cancelled.emit()">
              {{ 'common.cancel' | transloco }}
            </button>
            <button type="button" class="btn btn--danger" autofocus (click)="confirmed.emit()">
              {{ confirmKey() | transloco }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .overlay {
      position: fixed;
      inset: 0;
      z-index: 40;
      display: grid;
      place-items: center;
      padding: 16px;
      background: rgba(23, 61, 45, 0.4);
      backdrop-filter: blur(4px);
    }

    .dialog {
      width: 100%;
      max-width: 420px;
      padding: 24px;
      border-radius: var(--radius-lg);
      background: var(--color-surface);
      box-shadow: var(--shadow-md);
    }

    .dialog__message {
      margin: 4px 0 20px;
      color: var(--color-text-muted);
      font-size: var(--text-sm);
    }

    .dialog__actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
  `,
})
export class ConfirmDialog {
  readonly open = input.required<boolean>();
  readonly messageKey = input.required<string>();
  readonly confirmKey = input('common.delete');

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  private readonly dialogPanel = viewChild<ElementRef<HTMLElement>>('dialog');

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    if (this.open()) {
      this.cancelled.emit();
    }
  }

  @HostListener('document:keydown', ['$event'])
  protected onKeydown(event: KeyboardEvent): void {
    if (!this.open() || event.key !== 'Tab') {
      return;
    }
    const panel = this.dialogPanel()?.nativeElement;
    if (!panel) {
      return;
    }
    const focusables = Array.from(
      panel.querySelectorAll<HTMLElement>(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
      ),
    );
    if (focusables.length === 0) {
      return;
    }
    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    const active = document.activeElement as HTMLElement | null;
    if (event.shiftKey) {
      if (active === first || !panel.contains(active)) {
        event.preventDefault();
        last.focus();
      }
    } else if (active === last || !panel.contains(active)) {
      event.preventDefault();
      first.focus();
    }
  }
}
