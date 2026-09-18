import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { RegionService } from '../../../core/i18n/region.service';
import { AuthService } from '../../../core/services/auth.service';
import { PublicPropertyService } from '../../../core/services/public-property.service';
import { SeoService } from '../../../core/seo/seo.service';
import { Icon } from '../../../shared/components/icon/icon';
import { PropertyMedia } from '../../../shared/components/property-media/property-media';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { MoneyPipe } from '../../../shared/pipes/money.pipe';
import { PublicProperty } from '../../../shared/models/public-property';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, TranslocoPipe, ReactiveFormsModule, Icon, PropertyMedia, Skeleton, MoneyPipe],
  templateUrl: './landing.html',
})
export class Landing implements OnInit, OnDestroy {
  private readonly publicApi = inject(PublicPropertyService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly seo = inject(SeoService);
  protected readonly region = inject(RegionService);

  protected readonly featured = signal<PublicProperty[]>([]);
  protected readonly loading = signal(true);
  protected readonly year = new Date().getFullYear();

  protected readonly searchForm = this.fb.nonNullable.group({ query: [''] });

  ngOnInit(): void {
    this.seo.setPage({ titleKey: 'landing.title', descriptionKey: 'landing.metaDescription', path: '/' });
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

  ngOnDestroy(): void {
    this.seo.clear();
  }

  protected search(): void {
    const query = this.searchForm.getRawValue().query;
    void this.router.navigate(['/search'], { queryParams: query.trim() === '' ? {} : { query } });
  }
}
