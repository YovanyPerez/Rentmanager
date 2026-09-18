import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { OwnerService } from '../../../core/services/owner.service';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Owner } from '../../../shared/models/owner';

@Component({
  selector: 'app-owners-list',
  imports: [RouterLink, TranslocoPipe, Icon, Skeleton, EmptyState, ConfirmDialog],
  templateUrl: './owners-list.html',
})
export class OwnersList implements OnInit {
  private readonly ownersApi = inject(OwnerService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);

  protected readonly owners = signal<Owner[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly confirming = signal<Owner | null>(null);

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    return this.owners().filter(
      (owner) =>
        term === '' ||
        owner.fullName.toLowerCase().includes(term) ||
        (owner.email ?? '').toLowerCase().includes(term) ||
        (owner.phone ?? '').toLowerCase().includes(term),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.ownersApi.list().subscribe({
      next: (owners) => {
        this.owners.set(owners);
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
    const owner = this.confirming();
    if (!owner) {
      return;
    }
    this.ownersApi.delete(owner.id).subscribe({
      next: () => {
        this.confirming.set(null);
        this.toasts.success('messages.ownerDeleted');
        this.load();
      },
      error: (error: unknown) => {
        this.confirming.set(null);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }
}
