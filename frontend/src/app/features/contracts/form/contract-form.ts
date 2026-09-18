import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { ContractService } from '../../../core/services/contract.service';
import { PropertyService } from '../../../core/services/property.service';
import { TenantService } from '../../../core/services/tenant.service';
import { fieldErrorMessage } from '../../../shared/forms/field-error';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { ContractRequest } from '../../../shared/models/contract';
import { Property } from '../../../shared/models/property';
import { Tenant } from '../../../shared/models/tenant';

@Component({
  selector: 'app-contract-form',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe],
  templateUrl: './contract-form.html',
})
export class ContractForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly contractsApi = inject(ContractService);
  private readonly propertiesApi = inject(PropertyService);
  private readonly tenantsApi = inject(TenantService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(LanguageService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly id = signal<number | null>(null);
  protected readonly properties = signal<Property[]>([]);
  protected readonly tenants = signal<Tenant[]>([]);
  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    propertyId: [0, [Validators.required, Validators.min(1)]],
    tenantId: [0, [Validators.required, Validators.min(1)]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    monthlyRent: [0, [Validators.required, Validators.min(0.01)]],
  });

  ngOnInit(): void {
    this.propertiesApi.list().subscribe({
      next: (properties) => this.properties.set(properties),
      error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
    });
    this.tenantsApi.list().subscribe({
      next: (tenants) => this.tenants.set(tenants),
      error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.contractsApi.get(id).subscribe({
        next: (contract) =>
          this.form.patchValue({
            propertyId: contract.propertyId,
            tenantId: contract.tenantId,
            startDate: contract.startDate,
            endDate: contract.endDate,
            monthlyRent: contract.monthlyRent,
          }),
        error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
      });
    }
  }

  protected fieldError(field: 'propertyId' | 'tenantId' | 'startDate' | 'endDate' | 'monthlyRent'): string | null {
    return fieldErrorMessage(this.form.controls[field], this.i18n);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorKey.set(null);

    const request: ContractRequest = this.form.getRawValue();
    const id = this.id();
    const save = id === null ? this.contractsApi.create(request) : this.contractsApi.update(id, request);

    save.subscribe({
      next: () => {
        this.toasts.success(id === null ? 'messages.contractCreated' : 'messages.contractUpdated');
        void this.router.navigateByUrl('/contracts');
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.toasts.error(this.apiErrors.keyOf(error));
        this.submitting.set(false);
      },
    });
  }
}
