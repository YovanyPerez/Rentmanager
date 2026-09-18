import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { RegionService } from '../../../core/i18n/region.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { PaymentService } from '../../../core/services/payment.service';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Icon } from '../../../shared/components/icon/icon';
import { Skeleton } from '../../../shared/components/skeleton/skeleton';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { MoneyPipe } from '../../../shared/pipes/money.pipe';
import { Payment, PaymentStatus } from '../../../shared/models/payment';

@Component({
  selector: 'app-payments-list',
  imports: [RouterLink, TranslocoPipe, DatePipe, Icon, Skeleton, StatusBadge, EmptyState, ConfirmDialog, MoneyPipe],
  templateUrl: './payments-list.html',
})
export class PaymentsList implements OnInit {
  private readonly paymentsApi = inject(PaymentService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  protected readonly auth = inject(AuthService);
  protected readonly region = inject(RegionService);

  protected readonly payments = signal<Payment[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly statusFilter = signal<PaymentStatus | 'ALL'>('ALL');
  protected readonly confirming = signal<Payment | null>(null);
  protected readonly statuses: PaymentStatus[] = ['PENDING', 'PAID', 'OVERDUE', 'CANCELLED'];

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.payments().filter(
      (payment) =>
        (status === 'ALL' || payment.status === status) &&
        (term === '' ||
          payment.propertyAddress.toLowerCase().includes(term) ||
          payment.tenantName.toLowerCase().includes(term)),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.paymentsApi.list().subscribe({
      next: (payments) => {
        this.payments.set(payments);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.loading.set(false);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }

  protected markPaid(payment: Payment): void {
    this.paymentsApi.update(payment.id, { status: 'PAID' }).subscribe({
      next: () => {
        this.toasts.success('messages.paymentPaid');
        this.load();
      },
      error: (error: unknown) => this.toasts.error(this.apiErrors.keyOf(error)),
    });
  }

  protected confirmCancel(): void {
    const payment = this.confirming();
    if (!payment) {
      return;
    }
    this.paymentsApi.update(payment.id, { status: 'CANCELLED' }).subscribe({
      next: () => {
        this.confirming.set(null);
        this.toasts.success('messages.paymentCancelled');
        this.load();
      },
      error: (error: unknown) => {
        this.confirming.set(null);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }
}
