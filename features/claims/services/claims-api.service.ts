import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Claim,
  CreateClaimRequest,
  UpdateClaimStatusRequest,
  Recovery,
  CreateRecoveryRequest,
  UpdateRecoveryStatusRequest
} from '../models/claims.models';

import { environment } from '../../../../environments/environments';

export interface CurrentUser {
  id?: number;
  userId?: number;
  username?: string;
  email?: string;
  role?: string;
  authorities?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ClaimsApiService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    `${environment.apiUrl}/api/claims`;

  private readonly authUrl =
    `${environment.apiUrl}/api/auth`;

  // ---------------- AUTHENTICATED USER ----------------

  getCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(
      `${this.authUrl}/me`,
      {
        withCredentials: true
      }
    );
  }

  // ---------------- CLAIMS ----------------

  createClaim(
    request: CreateClaimRequest
  ): Observable<Claim> {
    return this.http.post<Claim>(
      this.baseUrl,
      request,
      {
        withCredentials: true
      }
    );
  }

  getAllClaims(): Observable<Claim[]> {
    return this.http.get<Claim[]>(
      this.baseUrl,
      {
        withCredentials: true
      }
    );
  }

  getOwnerClaims(): Observable<Claim[]> {
    return this.http.get<Claim[]>(
      `${this.baseUrl}/owner`,
      {
        withCredentials: true
      }
    );
  }

  getClaim(
    claimId: number
  ): Observable<Claim> {
    return this.http.get<Claim>(
      `${this.baseUrl}/${claimId}`,
      {
        withCredentials: true
      }
    );
  }

  updateClaimStatus(
    claimId: number,
    request: UpdateClaimStatusRequest
  ): Observable<Claim> {
    return this.http.patch<Claim>(
      `${this.baseUrl}/${claimId}/status`,
      request,
      {
        withCredentials: true
      }
    );
  }

  // ---------------- RECOVERY ----------------

  createRecovery(
    claimId: number,
    request: CreateRecoveryRequest
  ): Observable<Recovery> {
    return this.http.post<Recovery>(
      `${this.baseUrl}/${claimId}/recovery`,
      request,
      {
        withCredentials: true
      }
    );
  }

  getClaimRecoveries(
    claimId: number
  ): Observable<Recovery[]> {
    return this.http.get<Recovery[]>(
      `${this.baseUrl}/${claimId}/recovery`,
      {
        withCredentials: true
      }
    );
  }

  getAllRecoveries(): Observable<Recovery[]> {
    return this.http.get<Recovery[]>(
      `${this.baseUrl}/recovery`,
      {
        withCredentials: true
      }
    );
  }

  updateRecoveryStatus(
    recoveryId: number,
    request: UpdateRecoveryStatusRequest
  ): Observable<Recovery> {
    return this.http.patch<Recovery>(
      `${this.baseUrl}/recovery/${recoveryId}/status`,
      request,
      {
        withCredentials: true
      }
    );
  }

  // ---------------- CLAIM DOCUMENTS ----------------

  uploadClaimDocument(
    claimId: number,
    file: File
  ): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post(
      `${this.baseUrl}/${claimId}/documents`,
      formData,
      {
        responseType: 'text',
        withCredentials: true
      }
    );
  }

  getClaimDocuments(
    claimId: number
  ): Observable<string[]> {
    return this.http.get<string[]>(
      `${this.baseUrl}/${claimId}/documents`,
      {
        withCredentials: true
      }
    );
  }

  downloadClaimDocument(
    claimId: number,
    filename: string
  ): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl}/${claimId}/documents/${encodeURIComponent(filename)}`,
      {
        responseType: 'blob',
        withCredentials: true
      }
    );
  }
}