import { DOCUMENT } from '@angular/core';
import { Injectable, inject, signal } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { TranslocoService } from '@jsverse/transloco';
import { environment } from '../../../environments/environment';

export interface PublicPageMeta {
  titleKey: string;
  descriptionKey: string;
  path: string;
}

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly meta = inject(Meta);
  private readonly document = inject(DOCUMENT);
  private readonly transloco = inject(TranslocoService);
  private readonly page = signal<PublicPageMeta | null>(null);

  constructor() {
    this.transloco.langChanges$.subscribe(() => this.apply());
  }

  setPage(page: PublicPageMeta): void {
    this.page.set(page);
    this.apply();
  }

  clear(): void {
    this.page.set(null);
    this.meta.removeTag('name="description"');
    for (const property of ['og:title', 'og:description', 'og:type', 'og:url']) {
      this.meta.removeTag(`property="${property}"`);
    }
    this.document.querySelector('link[rel="canonical"]')?.remove();
  }

  private apply(): void {
    const page = this.page();
    if (!page) {
      return;
    }
    const base = environment.publicBaseUrl.replace(/\/$/, '');
    const url = `${base}${page.path}`;
    const description = this.t(page.descriptionKey);
    this.meta.updateTag({ name: 'description', content: description });
    this.meta.updateTag({ property: 'og:title', content: this.t(page.titleKey) });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.setCanonical(url);
  }

  private t(key: string): string {
    return this.transloco.translate(key) ?? '';
  }

  private setCanonical(url: string): void {
    let link = this.document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.document.head.appendChild(link);
    }
    link.setAttribute('href', url);
  }
}
