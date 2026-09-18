import { AbstractControl } from '@angular/forms';
import { LanguageService } from '../../core/i18n/language.service';

/** Translated validation message for a form control, based on its current errors. */
export function fieldErrorMessage(control: AbstractControl, i18n: LanguageService): string | null {
  if (!control.touched || control.valid) {
    return null;
  }
  if (control.hasError('required')) {
    return i18n.t('validation.required');
  }
  if (control.hasError('email')) {
    return i18n.t('validation.email');
  }
  if (control.hasError('minlength')) {
    return i18n.t('validation.minLength', { min: control.getError('minlength').requiredLength });
  }
  if (control.hasError('maxlength')) {
    return i18n.t('validation.maxLength', { max: control.getError('maxlength').requiredLength });
  }
  return i18n.t('validation.invalid');
}
