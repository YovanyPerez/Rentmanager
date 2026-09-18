import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { RegionService } from '../../../core/i18n/region.service';
import { PublicPropertyService } from '../../../core/services/public-property.service';
import { SeoService } from '../../../core/seo/seo.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { PropertyMedia } from '../../../shared/components/property-media/property-media';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { MoneyPipe } from '../../../shared/pipes/money.pipe';
import { PublicProperty } from '../../../shared/models/public-property';

@Component({
  selector: 'app-public-search',
  imports: [RouterLink, TranslocoPipe, ReactiveFormsModule, Icon, PropertyMedia, Skeleton, EmptyState, MoneyPipe],
  templateUrl: './public-search.html',
})
export class PublicSearch implements OnInit, OnDestroy {
  private readonly publicApi = inject(PublicPropertyService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly seo = inject(SeoService);
  protected readonly region = inject(RegionService);

  protected readonly results = signal<PublicProperty[]>([]);
  protected readonly loading = signal(true);
  protected readonly searchForm = this.fb.nonNullable.group({ query: [''] });

  ngOnInit(): void {
    this.seo.setPage({ titleKey: 'search.title', descriptionKey: 'search.metaDescription', path: '/search' });
    this.route.queryParamMap.subscribe((params) => {
      const query = params.get('query') ?? '';
      this.searchForm.patchValue({ query });
      this.load(query);
    });
  }

  ngOnDestroy(): void {
    this.seo.clear();
  }

  protected search(): void {
    const query = this.searchForm.getRawValue().query.trim();
    void this.router.navigate(['/search'], { queryParams: query === '' ? {} : { query } });
  }

  private load(query: string): void {
    this.loading.set(true);
    this.publicApi.search(query).subscribe({
      next: (results) => {
        this.results.set(results);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
