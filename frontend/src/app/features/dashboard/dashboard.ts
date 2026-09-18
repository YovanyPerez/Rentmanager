import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { RegionService } from '../../core/i18n/region.service';
import { ApiErrorService } from '../../core/services/api-error.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { MaintenanceService } from '../../core/services/maintenance.service';
import { PaymentService } from '../../core/services/payment.service';
import { Icon } from '../../shared/components/icon/icon';
import { Skeleton } from '../../shared/components/skeleton/skeleton';
import { StatusBadge } from '../../shared/components/status-badge/status-badge';
import { ToastService } from '../../shared/components/toast/toast.service';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { DashboardStats } from '../../shared/models/dashboard';
import { Maintenance } from '../../shared/models/maintenance';
import { Payment } from '../../shared/models/payment';

interface ChartMonth {
  label: string;
  total: number;
}

@Component({
  selector: 'app-dashboard',
  imports: [TranslocoPipe, DatePipe, Skeleton, StatusBadge, Icon, MoneyPipe],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private readonly dashboardApi = inject(DashboardService);
  private readonly paymentsApi = inject(PaymentService);
  private readonly maintenanceApi = inject(MaintenanceService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  protected readonly auth = inject(AuthService);
  protected readonly region = inject(RegionService);

  protected readonly stats = signal<DashboardStats | null>(null);
  protected readonly payments = signal<Payment[]>([]);
  protected readonly requests = signal<Maintenance[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);

  private pendingCalls = 3;
  private readonly retried = new Set<string>();

  protected readonly upcoming = computed(() =>
    this.payments()
      .filter((payment) => payment.status === 'PENDING' || payment.status === 'OVERDUE')
      .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
      .slice(0, 5),
  );

  protected readonly recentRequests = computed(() =>
    this.requests()
      .filter((request) => request.status === 'OPEN' || request.status === 'IN_PROGRESS')
      .slice(0, 5),
  );

  protected readonly chart = computed<ChartMonth[]>(() => {
    const now = new Date();
    const currency = this.region.currency();
    const months: ChartMonth[] = [];
    for (let index = 11; index >= 0; index--) {
      const date = new Date(now.getFullYear(), now.getMonth() - index, 1);
      months.push({
        label: date.toLocaleDateString(this.region.locale(), { month: 'short' }),
        total: 0,
      });
    }
    for (const payment of this.payments()) {
      if (payment.status !== 'PAID' || !payment.paidDate || payment.currency !== currency) {
        continue;
      }
      const paid = new Date(payment.paidDate);
      const diff =
        (now.getFullYear() - paid.getFullYear()) * 12 + (now.getMonth() - paid.getMonth());
      if (diff < 0 || diff > 11) {
        continue;
      }
      months[11 - diff].total += payment.amount;
    }
    return months;
  });

  protected readonly maxChart = computed(() => Math.max(...this.chart().map((m) => m.total), 1));

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.retried.clear();
    this.pendingCalls = 3;
    this.loadStats();
    this.loadPayments();
    this.loadRequests();
  }

  private loadStats(): void {
    this.dashboardApi.statistics().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.complete();
      },
      error: (error: unknown) => this.fail('stats', error, () => this.loadStats()),
    });
  }

  private loadPayments(): void {
    this.paymentsApi.list().subscribe({
      next: (payments) => {
        this.payments.set(payments);
        this.complete();
      },
      error: (error: unknown) => this.fail('payments', error, () => this.loadPayments()),
    });
  }

  private loadRequests(): void {
    this.maintenanceApi.list().subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.complete();
      },
      error: (error: unknown) => this.fail('requests', error, () => this.loadRequests()),
    });
  }

  protected barHeight(month: ChartMonth): number {
    return Math.max(Math.round((month.total / this.maxChart()) * 100), 2);
  }

  private complete(): void {
    this.pendingCalls -= 1;
    if (this.pendingCalls === 0) {
      this.loading.set(false);
    }
  }

  /** One silent retry per call (covers a backend restarting); the second failure is shown. */
  private fail(name: string, error: unknown, retry: () => void): void {
    if (this.retried.has(name)) {
      const key = this.apiErrors.keyOf(error);
      this.errorKey.set(key);
      this.toasts.error(key);
      this.complete();
      return;
    }
    this.retried.add(name);
    setTimeout(retry, 1500);
  }
}
