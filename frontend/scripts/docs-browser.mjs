// Navegador del sistema para los scripts de documentación (Edge o Chrome, sin descargas).
import { existsSync } from 'node:fs';
import puppeteer from 'puppeteer-core';

export function findBrowser() {
  const candidates = [
    process.env.BROWSER_PATH,
    `${process.env['ProgramFiles(x86)']}\\Microsoft\\Edge\\Application\\msedge.exe`,
    `${process.env.ProgramFiles}\\Microsoft\\Edge\\Application\\msedge.exe`,
    `${process.env.ProgramFiles}\\Google\\Chrome\\Application\\chrome.exe`,
    `${process.env['ProgramFiles(x86)']}\\Google\\Chrome\\Application\\chrome.exe`,
  ].filter(Boolean);
  const found = candidates.find((path) => existsSync(path));
  if (!found) {
    throw new Error('No se encontró Edge/Chrome. Define BROWSER_PATH con la ruta del navegador.');
  }
  return found;
}

export function launchBrowser() {
  return puppeteer.launch({
    executablePath: findBrowser(),
    headless: true,
    args: ['--hide-scrollbars'],
  });
}
