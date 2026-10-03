import { Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import {
  RouterOutlet,
  RouterLink,
  RouterLinkActive
} from '@angular/router';
import { Store } from '@ngrx/store';

import { selectUser } from '../../../features/auth/store/auth.selector';
import { logout } from '../../../features/auth/store/auth.actions';
import { RoleLabelPipe } from '../../../shared/pipes/role-label.pipe';
import { NotificationComponent } from '../../../shared/notification/notification.component/notification.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    AsyncPipe,
    RoleLabelPipe,
    NotificationComponent
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css'
})
export class MainLayoutComponent {
  private readonly store = inject(Store);

  readonly user$ = this.store.select(selectUser);
  readonly currentYear = new Date().getFullYear();

  logout(): void {
    this.store.dispatch(logout());
  }
}