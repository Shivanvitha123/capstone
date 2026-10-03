import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { filter, map, switchMap, take } from 'rxjs';

import { UserRole } from '../constants/roles';
import { selectAuth } from '../../features/auth/store/auth.selector';
import { loadCurrentUser } from '../../features/auth/store/auth.actions';

export const roleGuard = (
  allowed: UserRole[]
): CanActivateFn => {
  return () => {
    const store = inject(Store);
    const router = inject(Router);

    return store.select(selectAuth).pipe(
      take(1),
      switchMap((state) => {
        if (!state.initialized) {
          store.dispatch(loadCurrentUser());
        }

        return store.select(selectAuth).pipe(
          filter((authState) => authState.initialized),
          take(1),
          map((authState) => {
            if (!authState.user) {
              return router.createUrlTree(['/auth/login']);
            }

            const role = String(authState.user.role ?? '')
              .replace(/^ROLE_/i, '')
              .toUpperCase();

            const permitted = allowed.some(
              (allowedRole) =>
                String(allowedRole)
                  .replace(/^ROLE_/i, '')
                  .toUpperCase() === role
            );

            return permitted
              ? true
              : router.createUrlTree(['/dashboard']);
          })
        );
      })
    );
  };
};