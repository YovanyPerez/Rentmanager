import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AuthService } from './core/services/auth.service';
import { Icon, IconName } from './shared/components/icon/icon';
import { LanguageSelector } from './shared/components/language-selector/language-selector';
import { RegionSelector } from './shared/components/region-selector/region-selector';
import { ToastHost } from './shared/components/toast/toast-host';

interface NavItem {
  path: string;
  icon: IconName;
  key: string;
}

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslocoPipe, LanguageSelector, RegionSelector, Icon, ToastHost],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly menuOpen = signal(false);

  protected readonly initials = computed(() => {
    const name = this.auth.user()?.fullName ?? '';
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  });

  protected readonly navItems = computed<NavItem[]>(() => {
    const role = this.auth.role();
    const items: NavItem[] = [{ path: '/home', icon: 'home', key: 'nav.home' }];
    if (role === 'ADMIN') {
      items.push({ path: '/dashboard', icon: 'bar-chart', key: 'dashboard.title' });
    }
    if (role === 'ADMIN' || role === 'OWNER') {
      items.push({ path: '/properties', icon: 'building', key: 'properties.title' });
    }
    if (role === 'ADMIN') {
      items.push(
        { path: '/owners', icon: 'user', key: 'owners.title' },
        { path: '/tenants', icon: 'users', key: 'tenants.title' },
      );
    }
    items.push(
      { path: '/contracts', icon: 'file-text', key: 'contracts.title' },
      { path: '/payments', icon: 'credit-card', key: 'payments.title' },
      { path: '/maintenance', icon: 'wrench', key: 'maintenance.title' },
    );
    return items;
  });

  protected readonly bottomNav = computed<NavItem[]>(() => {
    const role = this.auth.role();
    const second: NavItem =
      role === 'ADMIN' || role === 'OWNER'
        ? { path: '/properties', icon: 'building', key: 'properties.title' }
        : { path: '/contracts', icon: 'file-text', key: 'contracts.title' };
    return [
      { path: '/home', icon: 'home', key: 'nav.home' },
      second,
      { path: '/payments', icon: 'credit-card', key: 'payments.title' },
      { path: '/maintenance', icon: 'wrench', key: 'maintenance.title' },
    ];
  });

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closeMenu();
  }
}
