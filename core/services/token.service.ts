import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  /**
   * JWTs are now stored in HttpOnly cookies.
   * JavaScript must not read or store the token.
   *
   * Authentication status is determined by the
   * backend through the /api/auth/me endpoint.
   */

  clear(): void {
    // The backend clears the HttpOnly cookie on logout.
    // JavaScript cannot directly delete an HttpOnly cookie.
  }
}