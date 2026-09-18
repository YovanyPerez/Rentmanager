import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { RegionService } from '../../../core/i18n/region.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { OwnerService } from '../../../core/services/owner.service';
import { PropertyService } from '../../../core/services/property.service';
import { fieldErrorMessage } from '../../../shared/forms/field-error';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { CurrencyCode } from '../../../shared/models/currency';
import { Owner } from '../../../shared/models/owner';
import { PropertyImage, PropertyRequest } from '../../../shared/models/property';

@Component({
  selector: 'app-property-form',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe],
  templateUrl: './property-form.html',
})
export class PropertyForm implements OnInit {
  private static readonly MAX_IMAGES = 6;
  private static readonly MAX_IMAGE_BYTES = 5 * 1024 * 1024;
  private static readonly ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  private readonly fb = inject(FormBuilder);
  private readonly propertiesApi = inject(PropertyService);
  private readonly ownersApi = inject(OwnerService);
  private readonly apiErrors = inject(ApiErrorService);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(LanguageService);
  private readonly region = inject(RegionService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly canChooseOwner = computed(() => this.auth.role() === 'ADMIN');
  protected readonly currencies: CurrencyCode[] = ['COP', 'EUR', 'MXN', 'USD'];
  protected readonly id = signal<number | null>(null);
  protected readonly owners = signal<Owner[]>([]);
  protected readonly ownerName = signal<string | null>(null);
  protected readonly images = signal<PropertyImage[]>([]);
  protected readonly uploading = signal(false);
  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    ownerId: [0, [Validators.required, Validators.min(1)]],
    address: ['', [Validators.required]],
    city: ['', [Validators.required]],
    description: [''],
    currency: [this.region.currency(), [Validators.required]],
    monthlyRent: [0, [Validators.required, Validators.min(0.01)]],
  });

  ngOnInit(): void {
    if (this.canChooseOwner()) {
      this.ownersApi.list().subscribe({
        next: (owners) => this.owners.set(owners),
        error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
      });
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.propertiesApi.get(id).subscribe({
        next: (property) => {
          this.ownerName.set(property.ownerName);
          this.images.set(property.images);
          this.form.patchValue({
            ownerId: property.ownerId,
            address: property.address,
            city: property.city,
            description: property.description ?? '',
            currency: property.currency,
            monthlyRent: property.monthlyRent,
          });
        },
        error: (error: unknown) => this.errorKey.set(this.apiErrors.keyOf(error)),
      });
    }
  }

  protected fieldError(field: 'ownerId' | 'address' | 'city' | 'monthlyRent'): string | null {
    return fieldErrorMessage(this.form.controls[field], this.i18n);
  }

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const selected = Array.from(input.files ?? []);
    input.value = '';
    const id = this.id();
    if (id === null || selected.length === 0) {
      return;
    }
    const valid = selected.filter(
      (file) =>
        PropertyForm.ALLOWED_TYPES.includes(file.type) &&
        file.size <= PropertyForm.MAX_IMAGE_BYTES,
    );
    if (valid.length !== selected.length) {
      this.toasts.error('errors.UNSUPPORTED_IMAGE_TYPE');
    }
    const room = PropertyForm.MAX_IMAGES - this.images().length;
    if (valid.length > room) {
      this.toasts.error('errors.IMAGE_LIMIT_REACHED');
    }
    const queue = valid.slice(0, room);
    if (queue.length > 0) {
      this.uploading.set(true);
      this.uploadNext(queue, id, 0);
    }
  }

  protected removeImage(image: PropertyImage): void {
    const id = this.id();
    if (id === null) {
      return;
    }
    this.propertiesApi.deleteImage(id, image.id).subscribe({
      next: () => {
        this.images.update((list) =>
          list.filter((item) => item.id !== image.id).map((item, index) => ({ ...item, position: index })),
        );
        this.toasts.success('messages.photoRemoved');
      },
      error: (error: unknown) => this.toasts.error(this.apiErrors.keyOf(error)),
    });
  }

  protected makeCover(image: PropertyImage): void {
    const id = this.id();
    if (id === null) {
      return;
    }
    this.propertiesApi.setCover(id, image.id).subscribe({
      next: (images) => this.images.set(images),
      error: (error: unknown) => this.toasts.error(this.apiErrors.keyOf(error)),
    });
  }

  protected moveImage(image: PropertyImage, delta: number): void {
    const id = this.id();
    if (id === null) {
      return;
    }
    const list = [...this.images()];
    const index = list.findIndex((item) => item.id === image.id);
    const target = index + delta;
    if (index < 0 || target < 0 || target >= list.length) {
      return;
    }
    [list[index], list[target]] = [list[target], list[index]];
    this.propertiesApi.reorderImages(id, list.map((item) => item.id)).subscribe({
      next: (images) => this.images.set(images),
      error: (error: unknown) => this.toasts.error(this.apiErrors.keyOf(error)),
    });
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
      currency: value.currency,
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

  private uploadNext(queue: File[], id: number, index: number): void {
    if (index >= queue.length) {
      this.uploading.set(false);
      return;
    }
    this.propertiesApi.uploadImage(id, queue[index]).subscribe({
      next: (image) => {
        this.images.update((list) => [...list, image]);
        this.uploadNext(queue, id, index + 1);
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.toasts.error(this.apiErrors.keyOf(error));
      },
    });
  }
}
