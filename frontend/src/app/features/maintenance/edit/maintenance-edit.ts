import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { MaintenanceService } from '../../../core/services/maintenance.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Maintenance, MaintenanceStatus, MaintenanceUpdateRequest } from '../../../shared/models/maintenance';

@Component({
  selector: 'app-maintenance-edit',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe],
  templateUrl: './maintenance-edit.html',
})
export class MaintenanceEdit implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly maintenanceApi = inject(MaintenanceService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly request = signal<Maintenance | null>(null);
  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly statuses: MaintenanceStatus[] = ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  protected readonly form = this.fb.nonNullable.group({
    status: ['OPEN' as MaintenanceStatus, [Validators.required]],
    assignedTo: [''],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.maintenanceApi.get(id).subscribe({
      next: (request) => {
        this.request.set(request);
        this.form.patchValue({ status: request.status, assignedTo: request.assignedTo ?? '' });
      },
      error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
    });
  }

  protected submit(): void {
    const request = this.request();
    if (!request || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorKey.set(null);

    const value = this.form.getRawValue();
    const update: MaintenanceUpdateRequest = {
      status: value.status,
      assignedTo: value.assignedTo.trim() === '' ? null : value.assignedTo,
    };
    this.maintenanceApi.update(request.id, update).subscribe({
      next: () => {
        this.toasts.success('messages.maintenanceUpdated');
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
