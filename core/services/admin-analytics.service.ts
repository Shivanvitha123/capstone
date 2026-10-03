import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environments';

export interface MonthlyRevenue {
  month: string;
  premiumRevenue: number;
  policyCount: number;
}

export interface BusinessAnalytics {
  businessId: number;
  totalPolicies: number;
  activePolicies: number;
  totalPremium: number;
  totalClaims: number;
  totalClaimsPaid: number;
}

export interface AdminDashboard {
  totalPolicies: number;
  activePolicies: number;
  totalPremium: number;
  monthlyPremium: number;
  yearlyPremium: number;
  totalClaims: number;
  settledClaims: number;
  totalClaimedAmount: number;
  totalClaimsPaid: number;
  claimsToPremiumRatioPercent: number;

  collectedPremium: number;
  profitBeforeOperatingExpenses: number;

  monthlyRevenue: MonthlyRevenue[];
  businessAnalytics: BusinessAnalytics[];
}

@Injectable({
  providedIn: 'root'
})
export class AdminAnalyticsService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl =
    `${environment.apiUrl}/api/admin/analytics`;

  getDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(
      `${this.apiUrl}/dashboard`,
      {
        withCredentials: true
      }
    );
  }
}