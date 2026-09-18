import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { PublicPropertyService } from '../../../core/services/public-property.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { PublicProperty } from '../../../shared/models/public-property';

@Component({
  selector: 'app-public-search',
  imports: [RouterLink, TranslocoPipe, DecimalPipe, ReactiveFormsModule, Icon, Skeleton, EmptyState],
  templateUrl: './public-search.html',
})
export class PublicSearch implements OnInit {
  private readonly publicApi = inject(PublicPropertyService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  protected readonly i18n = inject(LanguageService);

  protected readonly results = signal<PublicProperty[]>([]);
  protected readonly loading = signal(true);
  protected readonly searchForm = this.fb.nonNullable.group({ query: [''] });

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const query = params.get('query') ?? '';
      this.searchForm.patchValue({ query });
      this.load(query);
    });
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
