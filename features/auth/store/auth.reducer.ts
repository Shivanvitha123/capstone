import { createReducer, on } from '@ngrx/store';

import {
  login,
  loginSuccess,
  loginFailure,
  register,
  registerSuccess,
  registerFailure,
  loadCurrentUser,
  currentUserSuccess,
  currentUserFailure,
  logout
} from './auth.actions';

import { AuthResponse } from '../models/auth.model';

export interface AuthState {
  user: AuthResponse | null;
  loading: boolean;
  initialized: boolean;
  error: string | null;
}

export const initialAuthState: AuthState = {
  user: null,
  loading: false,
  initialized: false,
  error: null
};

export const authReducer = createReducer(
  initialAuthState,

  on(login, register, state => ({
    ...state,
    loading: true,
    error: null
  })),

  on(loginSuccess, registerSuccess, (state, { user }) => ({
    ...state,
    user,
    loading: false,
    initialized: true,
    error: null
  })),

  on(loginFailure, registerFailure, (state, { error }) => ({
    ...state,
    user: null,
    loading: false,
    initialized: true,
    error
  })),

  on(loadCurrentUser, state => ({
    ...state,
    loading: true,
    error: null
  })),

  on(currentUserSuccess, (state, { user }) => ({
    ...state,
    user,
    loading: false,
    initialized: true,
    error: null
  })),

  on(currentUserFailure, state => ({
    ...state,
    user: null,
    loading: false,
    initialized: true,
    error: null
  })),

  on(logout, () => ({
    ...initialAuthState,
    initialized: true
  }))
);