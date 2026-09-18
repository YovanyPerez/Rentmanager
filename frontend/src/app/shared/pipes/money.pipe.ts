import { Pipe, PipeTransform } from '@angular/core';
import { formatCurrency, getCurrencySymbol } from '@angular/common';
import { CurrencyCode } from '../models/currency';

/** Formats an amount with the record currency and the active locale (symbol included). */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(
    value: number | null | undefined,
    currency: CurrencyCode | string | null | undefined,
    locale: string,
  ): string {
    if (value === null || value === undefined) {
      return '—';
    }
    const code = currency ?? 'COP';
    const symbol = getCurrencySymbol(code, 'narrow', locale);
    return formatCurrency(value, locale, symbol, code, '1.0-2');
  }
}
