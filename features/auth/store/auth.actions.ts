import { createAction, props } from '@ngrx/store';

import {
  AuthResponse,
  LoginRequest,
  RegisterRequest
} from '../models/auth.model';

export const login = createAction(
  '[Auth] Login',
  props<{ request: LoginRequest }>()
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ user: AuthResponse }>()
);

export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: string }>()
);

export const register = createAction(
  '[Auth] Register',
  props<{ request: RegisterRequest }>()
);

export const registerSuccess = createAction(
  '[Auth] Register Success',
  props<{ user: AuthResponse }>()
);

export const registerFailure = createAction(
  '[Auth] Register Failure',
  props<{ error: string }>()
);

export const loadCurrentUser = createAction(
  '[Auth] Load Current User'
);

export const currentUserSuccess = createAction(
  '[Auth] Current User Success',
  props<{ user: AuthResponse }>()
);

export const currentUserFailure = createAction(
  '[Auth] Current User Failure'
);

export const logout = createAction(
  '[Auth] Logout'
);