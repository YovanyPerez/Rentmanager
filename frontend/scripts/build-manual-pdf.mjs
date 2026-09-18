// Genera docs/manual-usuario.pdf a partir del README.md (imágenes incluidas).
// Uso: npm run docs:pdf   (desde frontend/)
import { existsSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { marked } from 'marked';
import { launchBrowser } from './docs-browser.mjs';

const ROOT = resolve(import.meta.dirname, '..', '..');
const README = resolve(ROOT, 'README.md');
const OUTPUT = resolve(ROOT, 'docs', 'manual-usuario.pdf');
const TEMP_HTML = resolve(ROOT, '.manual-build.html');

const body = marked.parse(readFileSync(README, 'utf8'));

const missing = [...body.matchAll(/<img[^>]+src="([^"]+)"/g)]
  .map((match) => match[1])
  .filter((src) => !/^https?:/.test(src) && !existsSync(resolve(ROOT, src)));
if (missing.length > 0) {
  console.error('Faltan imágenes referenciadas en README.md:');
  for (const src of missing) console.error(`  ${src}`);
  process.exit(1);
}

const html = `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>RentManager — Manual de usuario</title>
<style>
  body { font-family: 'Segoe UI', Arial, sans-serif; color: #1f2a28; line-height: 1.55; }
  h1, h2, h3 { color: #16594a; break-after: avoid; }
  h1 { font-size: 26px; }
  h2 { font-size: 21px; margin-top: 28px; }
  h3 { font-size: 17px; margin-top: 22px; }
  img { max-width: 100%; border: 1px solid #dde5e2; border-radius: 6px; margin: 8px 0 18px; break-inside: avoid; }
  table { border-collapse: collapse; width: 100%; margin: 10px 0 18px; break-inside: avoid; }
  th, td { border: 1px solid #dde5e2; padding: 6px 10px; text-align: left; font-size: 13px; }
  th { background: #f2f6f4; }
  code { background: #f2f6f4; padding: 1px 5px; border-radius: 4px; font-size: 13px; }
  pre { background: #f2f6f4; padding: 10px; border-radius: 6px; overflow-x: auto; }
  pre code { background: none; padding: 0; }
</style>
</head>
<body>${body}</body>
</html>`;

writeFileSync(TEMP_HTML, html);
const browser = await launchBrowser();
try {
  const page = await browser.newPage();
  await page.goto(pathToFileURL(TEMP_HTML).href, { waitUntil: 'load' });
  await page.pdf({
    path: OUTPUT,
    format: 'A4',
    printBackground: true,
    margin: { top: '18mm', bottom: '20mm', left: '16mm', right: '16mm' },
    displayHeaderFooter: true,
    headerTemplate: '<span></span>',
    footerTemplate:
      '<div style="width:100%;text-align:center;font-size:9px;color:#888">' +
      'RentManager — Manual de usuario — página <span class="pageNumber"></span> de <span class="totalPages"></span></div>',
  });
} finally {
  await browser.close();
  rmSync(TEMP_HTML, { force: true });
}

console.log(`PDF generado: ${OUTPUT} (${Math.round(statSync(OUTPUT).size / 1024)} KB)`);
