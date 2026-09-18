import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  kind: 'success' | 'error';
  key: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;

  readonly toasts = signal<Toast[]>([]);

  success(key: string): void {
    this.push('success', key);
  }

  error(key: string): void {
    this.push('error', key);
  }

  dismiss(id: number): void {
    this.toasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private push(kind: Toast['kind'], key: string): void {
    const id = this.nextId++;
    this.toasts.update((toasts) => [...toasts, { id, kind, key }]);
    setTimeout(() => this.dismiss(id), 4000);
  }
}
