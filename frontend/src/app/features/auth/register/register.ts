import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { fieldErrorMessage } from '../../../shared/forms/field-error';
import { Icon } from '../../../shared/components/icon/icon';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe, Icon],
  templateUrl: './register.html',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly i18n = inject(LanguageService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected fieldError(field: 'fullName' | 'email' | 'password'): string | null {
    return fieldErrorMessage(this.form.controls[field], this.i18n);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorKey.set(null);
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl('/home'),
      error: (error: unknown) => {
        this.errorKey.set(this.apiErrors.keyOf(error));
        this.submitting.set(false);
      },
    });
  }
}
