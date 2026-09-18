import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { PropertyService } from '../../../core/services/property.service';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Property, PropertyStatus } from '../../../shared/models/property';

@Component({
  selector: 'app-properties-list',
  imports: [
    RouterLink,
    TranslocoPipe,
    DecimalPipe,
    Icon,
    Skeleton,
    StatusBadge,
    EmptyState,
    ConfirmDialog,
  ],
  templateUrl: './properties-list.html',
})
export class PropertiesList implements OnInit {
  private readonly propertiesApi = inject(PropertyService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  protected readonly auth = inject(AuthService);
  protected readonly i18n = inject(LanguageService);

  protected readonly properties = signal<Property[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly statusFilter = signal<PropertyStatus | 'ALL'>('ALL');
  protected readonly confirming = signal<Property | null>(null);
  protected readonly statuses: PropertyStatus[] = ['AVAILABLE', 'RENTED', 'MAINTENANCE', 'INACTIVE'];

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.properties().filter(
      (property) =>
        (status === 'ALL' || property.status === status) &&
        (term === '' ||
          property.address.toLowerCase().includes(term) ||
          property.city.toLowerCase().includes(term) ||
          property.ownerName.toLowerCase().includes(term)),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.propertiesApi.list().subscribe({
      next: (properties) => {
        this.properties.set(properties);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.loading.set(false);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }

  protected changeStatus(property: Property, status: string): void {
    this.propertiesApi.changeStatus(property.id, status as PropertyStatus).subscribe({
      next: () => {
        this.toasts.success('common.saved');
        this.load();
      },
      error: (error: unknown) => this.toasts.error(this.apiErrors.keyOf(error)),
    });
  }

  protected requestDeactivate(property: Property): void {
    this.confirming.set(property);
  }

  protected confirmDeactivate(): void {
    const property = this.confirming();
    if (!property) {
      return;
    }
    this.propertiesApi.deactivate(property.id).subscribe({
      next: () => {
        this.confirming.set(null);
        this.toasts.success('messages.propertyDeactivated');
        this.load();
      },
      error: (error: unknown) => {
        this.confirming.set(null);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }
}
