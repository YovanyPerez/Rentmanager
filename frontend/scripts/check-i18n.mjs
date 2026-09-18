import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const i18nDir = resolve(import.meta.dirname, '..', 'public', 'assets', 'i18n');

function flatten(value, prefix = '') {
  return Object.entries(value).flatMap(([key, child]) =>
    child !== null && typeof child === 'object' && !Array.isArray(child)
      ? flatten(child, `${prefix}${key}.`)
      : [`${prefix}${key}`],
  );
}

function keysOf(file) {
  return new Set(flatten(JSON.parse(readFileSync(resolve(i18nDir, file), 'utf8'))));
}

const es = keysOf('es.json');
const en = keysOf('en.json');
const missingInEn = [...es].filter((key) => !en.has(key));
const missingInEs = [...en].filter((key) => !es.has(key));

if (missingInEn.length > 0 || missingInEs.length > 0) {
  console.error('i18n key parity FAILED');
  if (missingInEn.length > 0) {
    console.error(`  missing in en.json (${missingInEn.length}): ${missingInEn.join(', ')}`);
  }
  if (missingInEs.length > 0) {
    console.error(`  missing in es.json (${missingInEs.length}): ${missingInEs.join(', ')}`);
  }
  process.exit(1);
}

console.log(`i18n key parity OK (${es.size} keys in es.json and en.json)`);
