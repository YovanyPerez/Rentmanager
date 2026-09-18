import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { TenantService } from '../../../core/services/tenant.service';
import { UserService } from '../../../core/services/user.service';
import { fieldErrorMessage } from '../../../shared/forms/field-error';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { TenantRequest } from '../../../shared/models/tenant';
import { UserSummary } from '../../../shared/models/user';

@Component({
  selector: 'app-tenant-form',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe],
  templateUrl: './tenant-form.html',
})
export class TenantForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly tenantsApi = inject(TenantService);
  private readonly usersApi = inject(UserService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(LanguageService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly id = signal<number | null>(null);
  protected readonly users = signal<UserSummary[]>([]);
  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.email]],
    phone: [''],
    userId: [null as number | null],
  });

  ngOnInit(): void {
    this.usersApi.list().subscribe({
      next: (users) => this.users.set(users.filter((user) => user.role === 'TENANT')),
      error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.tenantsApi.get(id).subscribe({
        next: (tenant) =>
          this.form.patchValue({
            fullName: tenant.fullName,
            email: tenant.email ?? '',
            phone: tenant.phone ?? '',
            userId: tenant.userId,
          }),
        error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
      });
    }
  }

  protected fieldError(field: 'fullName' | 'email'): string | null {
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
    const request: TenantRequest = {
      fullName: value.fullName,
      email: value.email.trim() === '' ? null : value.email,
      phone: value.phone.trim() === '' ? null : value.phone,
      userId: value.userId,
    };
    const id = this.id();
    const save = id === null ? this.tenantsApi.create(request) : this.tenantsApi.update(id, request);

    save.subscribe({
      next: () => {
        this.toasts.success(id === null ? 'messages.tenantCreated' : 'messages.tenantUpdated');
        void this.router.navigateByUrl('/tenants');
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.toasts.error(this.apiErrors.keyOf(error));
        this.submitting.set(false);
      },
    });
  }
}
