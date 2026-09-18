import { Injectable, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class RobotsService {
  private readonly router = inject(Router);
  private readonly meta = inject(Meta);

  constructor() {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.apply());
    this.apply();
  }

  private apply(): void {
    let node: ActivatedRouteSnapshot | null = this.router.routerState.snapshot.root;
    let isPublic = false;
    while (node) {
      if (node.data['public'] === true) {
        isPublic = true;
      }
      node = node.firstChild;
    }

    if (isPublic) {
      this.meta.removeTag('name="robots"');
    } else {
      this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
    }
  }
}
