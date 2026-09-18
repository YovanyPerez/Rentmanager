import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { AuthService } from '../../../core/services/auth.service';
import { PublicPropertyService } from '../../../core/services/public-property.service';
import { Icon } from '../../../shared/components/icon/icon';
import { PropertyMedia } from '../../../shared/components/property-media/property-media';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { PublicProperty } from '../../../shared/models/public-property';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, TranslocoPipe, DecimalPipe, ReactiveFormsModule, Icon, PropertyMedia, Skeleton],
  templateUrl: './landing.html',
})
export class Landing implements OnInit {
  private readonly publicApi = inject(PublicPropertyService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  protected readonly i18n = inject(LanguageService);

  protected readonly featured = signal<PublicProperty[]>([]);
  protected readonly loading = signal(true);
  protected readonly year = new Date().getFullYear();

  protected readonly searchForm = this.fb.nonNullable.group({ query: [''] });

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      void this.router.navigateByUrl('/home', { replaceUrl: true });
      return;
    }
    this.publicApi.search().subscribe({
      next: (properties) => {
        this.featured.set(properties.slice(0, 4));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected search(): void {
    const query = this.searchForm.getRawValue().query;
    void this.router.navigate(['/search'], { queryParams: query.trim() === '' ? {} : { query } });
  }
}
