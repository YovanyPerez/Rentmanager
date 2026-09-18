import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { OwnerService } from '../../../core/services/owner.service';
import { PropertyService } from '../../../core/services/property.service';
import { fieldErrorMessage } from '../../../shared/forms/field-error';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Owner } from '../../../shared/models/owner';
import { PropertyRequest } from '../../../shared/models/property';

@Component({
  selector: 'app-property-form',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe],
  templateUrl: './property-form.html',
})
export class PropertyForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly propertiesApi = inject(PropertyService);
  private readonly ownersApi = inject(OwnerService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(LanguageService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly id = signal<number | null>(null);
  protected readonly owners = signal<Owner[]>([]);
  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    ownerId: [0, [Validators.required, Validators.min(1)]],
    address: ['', [Validators.required]],
    city: ['', [Validators.required]],
    description: [''],
    imageUrl: ['', [Validators.maxLength(500)]],
    monthlyRent: [0, [Validators.required, Validators.min(0.01)]],
  });

  ngOnInit(): void {
    this.ownersApi.list().subscribe({
      next: (owners) => this.owners.set(owners),
      error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.propertiesApi.get(id).subscribe({
        next: (property) =>
          this.form.patchValue({
            ownerId: property.ownerId,
            address: property.address,
            city: property.city,
            description: property.description ?? '',
            imageUrl: property.imageUrl ?? '',
            monthlyRent: property.monthlyRent,
          }),
        error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
      });
    }
  }

  protected fieldError(field: 'ownerId' | 'address' | 'city' | 'imageUrl' | 'monthlyRent'): string | null {
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
    const request: PropertyRequest = {
      ownerId: value.ownerId,
      address: value.address,
      city: value.city,
      description: value.description.trim() === '' ? null : value.description,
      imageUrl: value.imageUrl.trim() === '' ? null : value.imageUrl.trim(),
      monthlyRent: value.monthlyRent,
    };
    const id = this.id();
    const save =
      id === null ? this.propertiesApi.create(request) : this.propertiesApi.update(id, request);

    save.subscribe({
      next: (property) => {
        this.toasts.success(id === null ? 'messages.propertyCreated' : 'messages.propertyUpdated');
        if (id === null) {
          void this.router.navigate(['/properties', property.id, 'edit']);
        } else {
          void this.router.navigateByUrl('/properties');
        }
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.toasts.error(this.apiErrors.keyOf(error));
        this.submitting.set(false);
      },
    });
  }
}
