import { Component, input } from '@angular/core';

@Component({
  selector: 'app-skeleton',
  template: `<span class="skeleton" [style.width]="width()" [style.height.px]="height()"></span>`,
  styles: `
    .skeleton {
      display: block;
      border-radius: var(--radius-sm);
      background: linear-gradient(
        90deg,
        var(--color-neutral-soft) 25%,
        #e3e9e7 37%,
        var(--color-neutral-soft) 63%
      );
      background-size: 400% 100%;
      animation: skeleton-shimmer 1.4s ease infinite;
    }

    @keyframes skeleton-shimmer {
      0% {
        background-position: 100% 50%;
      }
      100% {
        background-position: 0 50%;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .skeleton {
        animation: none;
      }
    }
  `,
})
export class Skeleton {
  readonly width = input('100%');
  readonly height = input(14);
}
