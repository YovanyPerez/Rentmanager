import { registerLocaleData } from '@angular/common';
import localeEn from '@angular/common/locales/en';
import localeEs from '@angular/common/locales/es';
import localeEsCo from '@angular/common/locales/es-CO';
import { MoneyPipe } from './money.pipe';

registerLocaleData(localeEs);
registerLocaleData(localeEn);
registerLocaleData(localeEsCo);

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('formats COP with the Colombian locale', () => {
    expect(pipe.transform(1800000, 'COP', 'es-CO')).toMatch(/^\$\s1\.800\.000$/);
  });

  it('formats EUR with the Spanish locale', () => {
    expect(pipe.transform(950, 'EUR', 'es')).toMatch(/^950\s€$/);
  });

  it('formats USD with the US locale', () => {
    expect(pipe.transform(1200, 'USD', 'en-US')).toBe('$1,200');
  });

  it('falls back to COP and handles empty values', () => {
    expect(pipe.transform(100, null, 'es-CO')).toMatch(/^\$\s100$/);
    expect(pipe.transform(null, 'COP', 'es-CO')).toBe('—');
  });
});
