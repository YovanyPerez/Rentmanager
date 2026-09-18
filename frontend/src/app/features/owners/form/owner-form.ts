import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { OwnerService } from '../../../core/services/owner.service';
import { UserService } from '../../../core/services/user.service';
import { fieldErrorMessage } from '../../../shared/forms/field-error';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { OwnerRequest } from '../../../shared/models/owner';
import { UserSummary } from '../../../shared/models/user';

@Component({
  selector: 'app-owner-form',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe],
  templateUrl: './owner-form.html',
})
export class OwnerForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ownersApi = inject(OwnerService);
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
      next: (users) => this.users.set(users.filter((user) => user.role === 'OWNER')),
      error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.ownersApi.get(id).subscribe({
        next: (owner) =>
          this.form.patchValue({
            fullName: owner.fullName,
            email: owner.email ?? '',
            phone: owner.phone ?? '',
            userId: owner.userId,
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
    const request: OwnerRequest = {
      fullName: value.fullName,
      email: value.email.trim() === '' ? null : value.email,
      phone: value.phone.trim() === '' ? null : value.phone,
      userId: value.userId,
    };
    const id = this.id();
    const save = id === null ? this.ownersApi.create(request) : this.ownersApi.update(id, request);

    save.subscribe({
      next: () => {
        this.toasts.success(id === null ? 'messages.ownerCreated' : 'messages.ownerUpdated');
        void this.router.navigateByUrl('/owners');
      },
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.toasts.error(this.apiErrors.keyOf(error));
        this.submitting.set(false);
      },
    });
  }
}
