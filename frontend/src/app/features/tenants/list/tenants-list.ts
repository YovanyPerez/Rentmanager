import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { TenantService } from '../../../core/services/tenant.service';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Tenant } from '../../../shared/models/tenant';

@Component({
  selector: 'app-tenants-list',
  imports: [RouterLink, TranslocoPipe, Icon, Skeleton, EmptyState, ConfirmDialog],
  templateUrl: './tenants-list.html',
})
export class TenantsList implements OnInit {
  private readonly tenantsApi = inject(TenantService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);

  protected readonly tenants = signal<Tenant[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly confirming = signal<Tenant | null>(null);

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    return this.tenants().filter(
      (tenant) =>
        term === '' ||
        tenant.fullName.toLowerCase().includes(term) ||
        (tenant.email ?? '').toLowerCase().includes(term) ||
        (tenant.phone ?? '').toLowerCase().includes(term),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.tenantsApi.list().subscribe({
      next: (tenants) => {
        this.tenants.set(tenants);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.loading.set(false);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }

  protected confirmDelete(): void {
    const tenant = this.confirming();
    if (!tenant) {
      return;
    }
    this.tenantsApi.delete(tenant.id).subscribe({
      next: () => {
        this.confirming.set(null);
        this.toasts.success('messages.tenantDeleted');
        this.load();
      },
      error: (error: unknown) => {
        this.confirming.set(null);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }
}
