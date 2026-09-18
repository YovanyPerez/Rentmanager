import { Component, computed, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

type BadgeKind = 'property' | 'contract' | 'payment' | 'maintenance';
type BadgeVariant = 'success' | 'info' | 'warning' | 'danger' | 'neutral';

const VARIANTS: Record<string, BadgeVariant> = {
  'property.AVAILABLE': 'success',
  'property.RENTED': 'info',
  'property.MAINTENANCE': 'warning',
  'property.INACTIVE': 'neutral',
  'contract.DRAFT': 'neutral',
  'contract.ACTIVE': 'success',
  'contract.EXPIRED': 'warning',
  'contract.TERMINATED': 'danger',
  'payment.PENDING': 'warning',
  'payment.PAID': 'success',
  'payment.OVERDUE': 'danger',
  'payment.CANCELLED': 'neutral',
  'maintenance.OPEN': 'warning',
  'maintenance.IN_PROGRESS': 'info',
  'maintenance.COMPLETED': 'success',
  'maintenance.CANCELLED': 'neutral',
};

@Component({
  selector: 'app-status-badge',
  imports: [TranslocoPipe],
  template: `<span [class]="cssClass()">{{ key() | transloco }}</span>`,
})
export class StatusBadge {
  readonly kind = input.required<BadgeKind>();
  readonly status = input.required<string>();

  protected readonly key = computed(() => `enums.${this.kind()}Status.${this.status()}`);

  protected readonly cssClass = computed(
    () => `chip chip--${VARIANTS[`${this.kind()}.${this.status()}`] ?? 'neutral'}`,
  );
}
