import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { PublicPropertyService } from '../../../core/services/public-property.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { PublicProperty } from '../../../shared/models/public-property';

@Component({
  selector: 'app-property-detail',
  imports: [RouterLink, TranslocoPipe, DecimalPipe, Icon, Skeleton, EmptyState],
  templateUrl: './property-detail.html',
})
export class PropertyDetail implements OnInit {
  private readonly publicApi = inject(PublicPropertyService);
  private readonly route = inject(ActivatedRoute);
  protected readonly i18n = inject(LanguageService);

  protected readonly property = signal<PublicProperty | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.publicApi.get(id).subscribe({
      next: (property) => {
        this.property.set(property);
        this.loading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }
}
