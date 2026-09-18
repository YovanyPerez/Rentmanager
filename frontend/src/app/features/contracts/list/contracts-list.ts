import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { RegionService } from '../../../core/i18n/region.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { ContractService } from '../../../core/services/contract.service';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { MoneyPipe } from '../../../shared/pipes/money.pipe';
import { Contract, ContractStatus } from '../../../shared/models/contract';

interface ConfirmAction {
  contract: Contract;
  action: 'delete' | 'terminate';
}

@Component({
  selector: 'app-contracts-list',
  imports: [RouterLink, TranslocoPipe, DatePipe, Icon, Skeleton, StatusBadge, EmptyState, ConfirmDialog, MoneyPipe],
  templateUrl: './contracts-list.html',
})
export class ContractsList implements OnInit {
  private readonly contractsApi = inject(ContractService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  protected readonly auth = inject(AuthService);
  protected readonly region = inject(RegionService);

  protected readonly contracts = signal<Contract[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly statusFilter = signal<ContractStatus | 'ALL'>('ALL');
  protected readonly confirming = signal<ConfirmAction | null>(null);
  protected readonly statuses: ContractStatus[] = ['DRAFT', 'ACTIVE', 'EXPIRED', 'TERMINATED'];

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.contracts().filter(
      (contract) =>
        (status === 'ALL' || contract.status === status) &&
        (term === '' ||
          contract.propertyAddress.toLowerCase().includes(term) ||
          contract.tenantName.toLowerCase().includes(term)),
    );
  });

  protected readonly confirmMessageKey = computed(() =>
    this.confirming()?.action === 'terminate'
      ? 'contracts.terminateConfirm'
      : 'contracts.deleteConfirm',
  );

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.contractsApi.list().subscribe({
      next: (contracts) => {
        this.contracts.set(contracts);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.loading.set(false);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }

  protected activate(contract: Contract): void {
    this.contractsApi.activate(contract.id).subscribe({
      next: () => {
        this.toasts.success('messages.contractActivated');
        this.load();
      },
      error: (error: unknown) => this.toasts.error(this.apiErrors.keyOf(error)),
    });
  }

  protected confirmAction(): void {
    const pending = this.confirming();
    if (!pending) {
      return;
    }
    const done = (): void => {
      this.confirming.set(null);
      this.toasts.success(
        pending.action === 'terminate' ? 'messages.contractTerminated' : 'messages.contractDeleted',
      );
      this.load();
    };
    const fail = (error: unknown): void => {
      this.confirming.set(null);
      this.toasts.error(this.apiErrors.keyOf(error));
    };

    if (pending.action === 'terminate') {
      this.contractsApi.terminate(pending.contract.id).subscribe({ next: done, error: fail });
    } else {
      this.contractsApi.delete(pending.contract.id).subscribe({ next: done, error: fail });
    }
  }
}
