import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { ContractService } from '../../../core/services/contract.service';
import { PaymentService } from '../../../core/services/payment.service';
import { fieldErrorMessage } from '../../../shared/forms/field-error';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Contract } from '../../../shared/models/contract';
import { PaymentRequest } from '../../../shared/models/payment';

@Component({
  selector: 'app-payment-form',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe],
  templateUrl: './payment-form.html',
})
export class PaymentForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly paymentsApi = inject(PaymentService);
  private readonly contractsApi = inject(ContractService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(LanguageService);
  private readonly router = inject(Router);

  protected readonly contracts = signal<Contract[]>([]);
  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    contractId: [0, [Validators.required, Validators.min(1)]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    dueDate: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.contractsApi.list().subscribe({
      next: (contracts) => this.contracts.set(contracts),
      error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
    });
  }

  protected fieldError(field: 'contractId' | 'amount' | 'dueDate'): string | null {
    return fieldErrorMessage(this.form.controls[field], this.i18n);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorKey.set(null);

    const request: PaymentRequest = this.form.getRawValue();
    this.paymentsApi.create(request).subscribe({
      next: () => {
        this.toasts.success('messages.paymentCreated');
        void this.router.navigateByUrl('/payments');
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.toasts.error(this.apiErrors.keyOf(error));
        this.submitting.set(false);
      },
    });
  }
}
