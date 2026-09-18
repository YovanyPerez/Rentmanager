import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { MaintenanceService } from '../../../core/services/maintenance.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Maintenance, MaintenanceStatus } from '../../../shared/models/maintenance';

@Component({
  selector: 'app-maintenance-list',
  imports: [RouterLink, TranslocoPipe, DatePipe, Icon, Skeleton, StatusBadge, EmptyState],
  templateUrl: './maintenance-list.html',
})
export class MaintenanceList implements OnInit {
  private readonly maintenanceApi = inject(MaintenanceService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  protected readonly auth = inject(AuthService);
  protected readonly i18n = inject(LanguageService);

  protected readonly requests = signal<Maintenance[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly statusFilter = signal<MaintenanceStatus | 'ALL'>('ALL');
  protected readonly statuses: MaintenanceStatus[] = ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.requests().filter(
      (request) =>
        (status === 'ALL' || request.status === status) &&
        (term === '' ||
          request.title.toLowerCase().includes(term) ||
          request.propertyAddress.toLowerCase().includes(term) ||
          request.createdByName.toLowerCase().includes(term)),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.maintenanceApi.list().subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.loading.set(false);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }

  protected changeStatus(request: Maintenance, status: string): void {
    this.maintenanceApi.update(request.id, { status: status as MaintenanceStatus }).subscribe({
      next: () => {
        this.toasts.success('messages.maintenanceUpdated');
        this.load();
      },
      error: (error: unknown) => this.toasts.error(this.apiErrors.keyOf(error)),
    });
  }
}
