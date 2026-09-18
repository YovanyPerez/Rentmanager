import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { ContractService } from '../../../core/services/contract.service';
import { MaintenanceService } from '../../../core/services/maintenance.service';
import { PropertyService } from '../../../core/services/property.service';
import { fieldErrorMessage } from '../../../shared/forms/field-error';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { MaintenanceCreateRequest } from '../../../shared/models/maintenance';

interface PropertyOption {
  id: number;
  label: string;
}

@Component({
  selector: 'app-maintenance-form',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe],
  templateUrl: './maintenance-form.html',
})
export class MaintenanceForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly maintenanceApi = inject(MaintenanceService);
  private readonly propertiesApi = inject(PropertyService);
  private readonly contractsApi = inject(ContractService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(LanguageService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly propertyOptions = signal<PropertyOption[]>([]);
  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    propertyId: [0, [Validators.required, Validators.min(1)]],
    title: ['', [Validators.required]],
    description: [''],
  });

  ngOnInit(): void {
    if (this.auth.role() === 'ADMIN') {
      this.propertiesApi.list().subscribe({
        next: (properties) =>
          this.propertyOptions.set(
            properties.map((property) => ({
              id: property.id,
              label: `${property.address} (${property.city})`,
            })),
          ),
        error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
      });
    } else {
      this.contractsApi.list().subscribe({
        next: (contracts) =>
          this.propertyOptions.set(
            contracts
              .filter((contract) => contract.status === 'ACTIVE')
              .map((contract) => ({ id: contract.propertyId, label: contract.propertyAddress })),
          ),
        error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
      });
    }
  }

  protected fieldError(field: 'propertyId' | 'title'): string | null {
    return fieldErrorMessage(this.form.controls[field], this.i18n);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorKey.set(null);

    const value = this.form.getRawValue();
    const request: MaintenanceCreateRequest = {
      propertyId: value.propertyId,
      title: value.title,
      description: value.description.trim() === '' ? null : value.description,
    };
    this.maintenanceApi.create(request).subscribe({
      next: () => {
        this.toasts.success('messages.maintenanceCreated');
        void this.router.navigateByUrl('/maintenance');
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.toasts.error(this.apiErrors.keyOf(error));
        this.submitting.set(false);
      },
    });
  }
}
