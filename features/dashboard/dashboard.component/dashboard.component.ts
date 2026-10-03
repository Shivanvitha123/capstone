import { Component, inject } from '@angular/core';
import {
  AsyncPipe,
  CurrencyPipe,
  DecimalPipe
} from '@angular/common';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import {
  Observable,
  Subject,
  combineLatest,
  of
} from 'rxjs';
import {
  catchError,
  map,
  startWith,
  switchMap
} from 'rxjs/operators';

import { selectUser } from '../../auth/store/auth.selector';
import {
  AdminAnalyticsService,
  AdminDashboard
} from '../../../core/services/admin-analytics.service';

interface DashboardViewState {
  loading: boolean;
  data: AdminDashboard | null;
  error: boolean;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    AsyncPipe,
    CurrencyPipe,
    DecimalPipe,
    RouterLink
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  private readonly store = inject(Store);
  private readonly analyticsService = inject(AdminAnalyticsService);

  readonly user$ = this.store.select(selectUser);

  private readonly refreshSubject = new Subject<void>();

  readonly dashboardState$: Observable<DashboardViewState> =
    combineLatest([
      this.user$,
      this.refreshSubject.pipe(startWith(undefined))
    ]).pipe(
      switchMap(([user]) => {
        const role = user?.role
          ?.replace(/^ROLE_/, '')
          .toUpperCase();

        if (role !== 'ADMIN') {
          return of({
            loading: false,
            data: null,
            error: false
          });
        }

        return this.analyticsService.getDashboard().pipe(
          map(data => ({
            loading: false,
            data,
            error: false
          })),
          startWith({
            loading: true,
            data: null,
            error: false
          }),
          catchError(error => {
            console.error(
              'Failed to load admin dashboard:',
              error
            );

            return of({
              loading: false,
              data: null,
              error: true
            });
          })
        );
      })
    );

  refreshDashboard(): void {
    this.refreshSubject.next();
  }

  getRole(role: string | undefined): string {
    if (!role) {
      return 'User';
    }

    return role
      .replace(/^ROLE_/, '')
      .replaceAll('_', ' ');
  }

  getMonthlyBarHeight(
    value: number | string | null | undefined,
    monthlyRevenue: AdminDashboard['monthlyRevenue']
  ): number {
    const currentValue = Number(value) || 0;

    if (currentValue <= 0 || !monthlyRevenue?.length) {
      return 0;
    }

    const maxValue = Math.max(
      ...monthlyRevenue.map(
        item => Number(item.premiumRevenue) || 0
      )
    );

    if (maxValue <= 0) {
      return 0;
    }

    return Math.max(
      4,
      (currentValue / maxValue) * 100
    );
  }

  formatPercentage(
    value: number | string | null | undefined
  ): string {
    const percentage = Number(value) || 0;

    return `${percentage.toFixed(2)}%`;
  }

  trackByMonth(
    index: number,
    item: AdminDashboard['monthlyRevenue'][number]
  ): string {
    return item.month || String(index);
  }

  trackByBusiness(
    index: number,
    item: AdminDashboard['businessAnalytics'][number]
  ): number | string {
    return item.businessId ?? index;
  }
}