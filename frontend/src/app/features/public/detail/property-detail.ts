import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { RegionService } from '../../../core/i18n/region.service';
import { PublicPropertyService } from '../../../core/services/public-property.service';
import { SeoService } from '../../../core/seo/seo.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { MoneyPipe } from '../../../shared/pipes/money.pipe';
import { PropertyImage } from '../../../shared/models/property';
import { PublicProperty } from '../../../shared/models/public-property';

@Component({
  selector: 'app-property-detail',
  imports: [RouterLink, TranslocoPipe, Icon, Skeleton, EmptyState, MoneyPipe],
  templateUrl: './property-detail.html',
})
export class PropertyDetail implements OnInit, OnDestroy {
  private readonly publicApi = inject(PublicPropertyService);
  private readonly route = inject(ActivatedRoute);
  private readonly seo = inject(SeoService);
  protected readonly region = inject(RegionService);

  protected readonly property = signal<PublicProperty | null>(null);
  protected readonly activeImage = signal<PropertyImage | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.seo.setPage({
      titleKey: 'detail.title',
      descriptionKey: 'detail.metaDescription',
      path: `/property/${id}`,
    });
    this.publicApi.get(id).subscribe({
      next: (property) => {
        this.property.set(property);
        this.activeImage.set(property.images[0] ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.seo.clear();
  }
}
