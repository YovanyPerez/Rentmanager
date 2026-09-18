import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';

/** Route titles are translation keys (AGENTS.md section 23: dynamic, localised titles). */
@Injectable({ providedIn: 'root' })
export class TranslatedTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly transloco = inject(TranslocoService);
  private lastKey: string | null = null;

  constructor() {
    super();
    this.transloco.langChanges$.subscribe(() => this.applyTitle());
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    this.lastKey = this.buildTitle(snapshot) ?? 'app.title';
    this.applyTitle();
  }

  private applyTitle(): void {
    this.title.setTitle(this.transloco.translate(this.lastKey ?? 'app.title'));
  }
}
