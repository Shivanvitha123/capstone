import { ApplicationConfig } from '@angular/core';

import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import { provideRouter } from '@angular/router';

import {
  provideStore,
  provideState
} from '@ngrx/store';

import { provideEffects } from '@ngrx/effects';

// Interceptors
import { authInterceptor }
  from './core/interceptors/auth.interceptor';

import { errorInterceptor }
  from './core/interceptors/error.interceptor';

// Reducers
import { authReducer }
  from './features/auth/store/auth.reducer';

import { businessReducer }
  from './features/business/store/business.reducer';

import { policyReducer }
  from './features/policies/store/policy.reducer';

import { riskReducer }
  from './features/risk/store/risk.reducer';

import { claimsReducer }
  from './features/claims/store/claims.reducer';

import { notificationReducer }
  from './features/notifications/store/notification.reducer';

// Effects
import { AuthEffects }
  from './features/auth/store/auth.effects';

import { BusinessEffects }
  from './features/business/store/business.effects';

import { PolicyEffects }
  from './features/policies/store/policy.effects';

import { RiskEffects }
  from './features/risk/store/risk.effects';

import { ClaimsEffects }
  from './features/claims/store/claims.effects';

import { NotificationEffects }
  from './features/notifications/store/notification.effects';

// Routes
import { routes }
  from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [

    // Router
    provideRouter(routes),

    // HTTP Client
    provideHttpClient(
      withInterceptors([
        authInterceptor,
        errorInterceptor
      ])
    ),

    // Root NgRx Store
    provideStore(),

    // Feature State Registration
    provideState(
      'auth',
      authReducer
    ),

    provideState(
      'business',
      businessReducer
    ),

    provideState(
      'policies',
      policyReducer
    ),

    provideState(
      'risk',
      riskReducer
    ),

    provideState(
      'claims',
      claimsReducer
    ),

    // Notifications Feature State
    provideState(
      'notifications',
      notificationReducer
    ),

    // NgRx Effects
    provideEffects(
      AuthEffects,
      BusinessEffects,
      PolicyEffects,
      RiskEffects,
      ClaimsEffects,
      NotificationEffects
    )

  ]
};