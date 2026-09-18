import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AuthService } from '../../core/services/auth.service';
import { Icon, IconName } from '../../shared/components/icon/icon';

interface QuickLink {
  path: string;
  icon: IconName;
  key: string;
}

@Component({
  selector: 'app-home',
  imports: [RouterLink, TranslocoPipe, Icon],
  templateUrl: './home.html',
})
export class Home {
  protected readonly auth = inject(AuthService);

  protected readonly links = computed<QuickLink[]>(() => {
    const role = this.auth.role();
    const links: QuickLink[] = [];
    if (role === 'ADMIN') {
      links.push({ path: '/dashboard', icon: 'bar-chart', key: 'dashboard.title' });
    }
    if (role === 'ADMIN' || role === 'OWNER') {
      links.push({ path: '/properties', icon: 'building', key: 'properties.title' });
      links.push({ path: '/owners', icon: 'user', key: 'owners.title' });
      links.push({ path: '/tenants', icon: 'users', key: 'tenants.title' });
    }
    links.push(
      { path: '/contracts', icon: 'file-text', key: 'contracts.title' },
      { path: '/payments', icon: 'credit-card', key: 'payments.title' },
      { path: '/maintenance', icon: 'wrench', key: 'maintenance.title' },
    );
    return role === 'ADMIN' ? links : links.filter((link) => link.path !== '/owners' && link.path !== '/tenants');
  });
}
