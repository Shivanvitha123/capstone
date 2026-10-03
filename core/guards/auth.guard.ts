import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router
} from '@angular/router';

import { Store } from '@ngrx/store';

import {
  filter,
  map,
  take
} from 'rxjs';

import { loadCurrentUser } from '../../features/auth/store/auth.actions';
import { selectAuth } from '../../features/auth/store/auth.selector';

export const authGuard: CanActivateFn = () => {
  const store = inject(Store);
  const router = inject(Router);

  store.dispatch(loadCurrentUser());

  return store.select(selectAuth).pipe(
    filter(state => state.initialized),
    take(1),
    map(state =>
      state.user
        ? true
        : router.createUrlTree(['/login'])
    )
  );
};