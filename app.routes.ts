import { Routes } from '@angular/router';

import { WelcomeComponent } from './features/welcome/welcome.component';

import { AuthLayoutComponent } from './layout/authLayout/auth-layout.component/auth-layout.component';
import { MainLayoutComponent } from './layout/mainLayout/main-layout.component/main-layout.component';

import { LoginComponent } from './features/auth/pages/login/login.component/login.component';
import { RegisterComponent } from './features/auth/pages/register/register.component/register.component';

import { DashboardComponent } from './features/dashboard/dashboard.component/dashboard.component';
import { BusinessComponent } from './features/business/components/business.component/business.component';
import { PoliciesComponent } from './features/policies/pages/policies.component/policies.component';
import { RiskComponent } from './features/risk/pages/risk.component/risk.component';
import { ClaimsComponent } from './features/claims/pages/claims.component/claims.component';

import { NotificationsComponent } from './features/notifications/pages/notifications.component/notifications.component';

import { UsersComponent } from './features/admin/users/users.component';
import { AuditComponent } from './features/admin/audit/audit.component';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // Welcome page
  {
    path: '',
    component: WelcomeComponent,
    pathMatch: 'full'
  },

  // Authentication pages
  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      {
        path: 'login',
        component: LoginComponent
      },
      {
        path: 'register',
        component: RegisterComponent
      }
    ]
  },

  // Protected application pages
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        component: DashboardComponent
      },
      {
        path: 'business',
        component: BusinessComponent
      },
      {
        path: 'policies',
        component: PoliciesComponent
      },
      {
        path: 'risk',
        component: RiskComponent
      },
      {
        path: 'claims',
        component: ClaimsComponent
      },
      {
        path: 'notifications',
        component: NotificationsComponent
      },
      {
        path: 'users',
        component: UsersComponent,
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN']
        }
      },
      {
        path: 'audit',
        component: AuditComponent,
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN']
        }
      }
    ]
  },

  // Redirect unknown routes
  {
    path: '**',
    redirectTo: ''
  }
];