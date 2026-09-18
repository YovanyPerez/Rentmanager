import { Component, input } from '@angular/core';
import { Icon } from '../icon/icon';

/** Property photo with the gradient placeholder fallback. */
@Component({
  selector: 'app-property-media',
  imports: [Icon],
  styles: `
    :host {
      display: contents;
    }
  `,
  template: `
    @if (url(); as src) {
      <img [src]="src" [alt]="alt()" loading="lazy" />
    } @else {
      <app-icon name="building" [size]="size()" />
    }
  `,
})
export class PropertyMedia {
  readonly url = input<string | null>(null);
  readonly alt = input('');
  readonly size = input(36);
}
