import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import {
  Actions,
  createEffect,
  ofType
} from '@ngrx/effects';

import {
  catchError,
  exhaustMap,
  map,
  of,
  tap
} from 'rxjs';

import { AuthApiService } from '../../../core/services/auth-api.service';

import * as AuthActions from './auth.actions';

@Injectable()
export class AuthEffects {
  private readonly actions$ = inject(Actions);
  private readonly authApi = inject(AuthApiService);
  private readonly router = inject(Router);

  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.login),
      exhaustMap(({ request }) =>
        this.authApi.login(request).pipe(
          map(user =>
            AuthActions.loginSuccess({ user })
          ),
          catchError(error =>
            of(
              AuthActions.loginFailure({
                error:
                  error?.error?.message ??
                  'Login failed. Please check your credentials.'
              })
            )
          )
        )
      )
    )
  );

  register$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.register),
      exhaustMap(({ request }) =>
        this.authApi.register(request).pipe(
          map(user =>
            AuthActions.registerSuccess({ user })
          ),
          catchError(error =>
            of(
              AuthActions.registerFailure({
                error:
                  error?.error?.message ??
                  'Registration failed. Please try again.'
              })
            )
          )
        )
      )
    )
  );

  loginSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.loginSuccess),
        tap(() => this.router.navigate(['/dashboard']))
      ),
    { dispatch: false }
  );

  registerSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.registerSuccess),
        tap(() => this.router.navigate(['/dashboard']))
      ),
    { dispatch: false }
  );

  loadCurrentUser$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.loadCurrentUser),
      exhaustMap(() =>
        this.authApi.me().pipe(
          map(user =>
            AuthActions.currentUserSuccess({ user })
          ),
          catchError(() =>
            of(AuthActions.currentUserFailure())
          )
        )
      )
    )
  );

  logout$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.logout),
        exhaustMap(() =>
          this.authApi.logout().pipe(
            catchError(() => of(void 0)),
            tap(() => this.router.navigate(['/']))
          )
        )
      ),
    { dispatch: false }
  );
}