import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { Icon } from '../../shared/components/icon/icon';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink, TranslocoPipe, Icon],
  template: `
    <div class="not-found">
      <app-icon name="map-pin" [size]="40" />
      <p class="not-found__code">404</p>
      <h1>{{ 'notFound.title' | transloco }}</h1>
      <p class="not-found__hint">{{ 'notFound.description' | transloco }}</p>
      <a class="btn btn--primary" routerLink="/">
        <app-icon name="home" [size]="16" />
        <span>{{ 'notFound.backHome' | transloco }}</span>
      </a>
    </div>
  `,
  styles: `
    .not-found {
      display: grid;
      justify-items: center;
      gap: 8px;
      max-width: 480px;
      margin: 0 auto;
      padding: 80px 24px;
      text-align: center;
      color: var(--color-text-muted);
    }

    .not-found__code {
      margin: 8px 0 0;
      font-size: var(--text-xl);
      font-weight: 700;
      color: var(--color-primary);
      letter-spacing: 0.08em;
    }

    .not-found__hint {
      margin: 0 0 16px;
      font-size: var(--text-sm);
    }
  `,
})
export class NotFound {}
