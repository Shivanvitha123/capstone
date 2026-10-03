import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import {
  CreatePolicyRequestDto,
  UpdatePolicyRequestDto,
  PolicyDto,
  StandardPolicyDto,
  CancellationRequestDto,
  CancellationReviewRequestDto,
  PaymentResponseDto
} from '../../../core/dtos';

import {
  CreatePolicyRequest,
  Policy,
  UpdatePolicyRequest,
  CancellationRequest,
  CancellationReviewRequest,
  StandardPolicy,
  PaymentResponse
} from '../models/policy.models';

import { environment } from '../../../../environments/environments';

@Injectable({
  providedIn: 'root'
})
export class PolicyApiService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    `${environment.apiUrl}/api/policies`;

  // =========================================================
  // STANDARD / PRELOADED POLICIES
  // =========================================================

  getStandardPolicies(): Observable<StandardPolicy[]> {

    return this.http
      .get<StandardPolicyDto[]>(
        `${this.baseUrl}/standard`,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((dtos) =>
          dtos.map((dto) =>
            this.mapStandardPolicy(dto)
          )
        )
      );
  }

  getStandardPolicy(
    id: string
  ): Observable<StandardPolicy> {

    return this.http
      .get<StandardPolicyDto>(
        `${this.baseUrl}/standard/${encodeURIComponent(id)}`,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((dto) =>
          this.mapStandardPolicy(dto)
        )
      );
  }

  // =========================================================
  // POLICY CREATION
  // =========================================================

  createPolicy(
    request: CreatePolicyRequest
  ): Observable<Policy> {

    const dto: CreatePolicyRequestDto = {
      businessId: request.businessId,
      policyNumber: request.policyNumber,
      policyType: request.policyType,
      coverageAmount: request.coverageAmount,
      coverageDetails: request.coverageDetails,
      coverageLocation: request.coverageLocation,
      premiumAmount: request.premiumAmount,
      startDate: request.startDate,
      tenureValue: request.tenureValue,
      tenureUnit: request.tenureUnit,
      paymentFrequency: request.paymentFrequency,
      description: request.description
    };

    return this.http
      .post<PolicyDto>(
        this.baseUrl,
        dto,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((response) =>
          this.mapPolicy(response)
        )
      );
  }

  // =========================================================
  // POLICY RETRIEVAL
  // =========================================================

  getPolicy(
    policyId: number
  ): Observable<Policy> {

    return this.http
      .get<PolicyDto>(
        `${this.baseUrl}/${policyId}`,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((dto) =>
          this.mapPolicy(dto)
        )
      );
  }

  getOwnerPolicies(): Observable<Policy[]> {

    return this.http
      .get<PolicyDto[]>(
        `${this.baseUrl}/owner`,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((dtos) =>
          dtos.map((dto) =>
            this.mapPolicy(dto)
          )
        )
      );
  }

  getAllPolicies(): Observable<Policy[]> {

    return this.http
      .get<PolicyDto[]>(
        this.baseUrl,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((dtos) =>
          dtos.map((dto) =>
            this.mapPolicy(dto)
          )
        )
      );
  }

  // =========================================================
  // PAYMENTS
  // =========================================================

  makePayment(
    policyId: number
  ): Observable<PaymentResponse> {

    return this.http
      .post<PaymentResponseDto>(
        `${environment.apiUrl}/api/payments/policies/${policyId}`,
        {},
        {
          withCredentials: true
        }
      )
      .pipe(
        map((dto) =>
          this.mapPayment(dto)
        )
      );
  }

  getPolicyPayments(
    policyId: number
  ): Observable<PaymentResponse[]> {

    return this.http
      .get<PaymentResponseDto[]>(
        `${environment.apiUrl}/api/payments/policies/${policyId}`,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((dtos) =>
          dtos.map((dto) =>
            this.mapPayment(dto)
          )
        )
      );
  }

  // =========================================================
  // POLICY UPDATE
  // =========================================================

  updatePolicy(
    policyId: number,
    request: UpdatePolicyRequest
  ): Observable<Policy> {

    const dto: UpdatePolicyRequestDto = {
      businessId: request.businessId,
      policyNumber: request.policyNumber,
      policyType: request.policyType,
      coverageAmount: request.coverageAmount,
      coverageDetails: request.coverageDetails,
      coverageLocation: request.coverageLocation,
      premiumAmount: request.premiumAmount,
      startDate: request.startDate,
      tenureValue: request.tenureValue,
      tenureUnit: request.tenureUnit,
      paymentFrequency: request.paymentFrequency,
      description: request.description
    };

    return this.http
      .put<PolicyDto>(
        `${this.baseUrl}/${policyId}`,
        dto,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((response) =>
          this.mapPolicy(response)
        )
      );
  }

  // =========================================================
  // POLICY SUBMISSION
  // =========================================================

  submitPolicy(
    policyId: number
  ): Observable<Policy> {

    return this.http
      .post<PolicyDto>(
        `${this.baseUrl}/${policyId}/submit`,
        {},
        {
          withCredentials: true
        }
      )
      .pipe(
        map((dto) =>
          this.mapPolicy(dto)
        )
      );
  }

  // =========================================================
  // POLICY STATUS
  // =========================================================

  updatePolicyStatus(
    policyId: number,
    status: string
  ): Observable<Policy> {

    return this.http
      .patch<PolicyDto>(
        `${this.baseUrl}/${policyId}/status`,
        {},
        {
          params: {
            status
          },
          withCredentials: true
        }
      )
      .pipe(
        map((dto) =>
          this.mapPolicy(dto)
        )
      );
  }

  // =========================================================
  // CANCELLATION
  // =========================================================

  requestCancellation(
    policyId: number,
    request: CancellationRequest
  ): Observable<Policy> {

    const dto: CancellationRequestDto = {
      reason: request.reason
    };

    return this.http
      .post<PolicyDto>(
        `${this.baseUrl}/${policyId}/cancellation`,
        dto,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((response) =>
          this.mapPolicy(response)
        )
      );
  }

  reviewCancellation(
    policyId: number,
    request: CancellationReviewRequest
  ): Observable<Policy> {

    const dto: CancellationReviewRequestDto = {
      approved: request.approved
    };

    return this.http
      .patch<PolicyDto>(
        `${this.baseUrl}/${policyId}/cancellation`,
        dto,
        {
          withCredentials: true
        }
      )
      .pipe(
        map((response) =>
          this.mapPolicy(response)
        )
      );
  }

  // =========================================================
  // DTO → MODEL MAPPERS
  // =========================================================

  private mapPolicy(
    dto: PolicyDto
  ): Policy {

    return {
      id: dto.id,
      businessId: dto.businessId,
      ownerId: dto.ownerId,

      policyNumber:
        dto.policyNumber,

      policyType:
        dto.policyType,

      coverageAmount:
        dto.coverageAmount,

      coverageDetails:
        dto.coverageDetails,

      coverageLocation:
        dto.coverageLocation,

      premiumAmount:
        dto.premiumAmount,

      startDate:
        dto.startDate,

      endDate:
        dto.endDate,

      tenureValue:
        dto.tenureValue,

      tenureUnit:
        dto.tenureUnit,

      paymentFrequency:
        dto.paymentFrequency,

      status:
        dto.status,

      cancellationReason:
        dto.cancellationReason,

      previousStatus:
        dto.previousStatus,

      description:
        dto.description,

      createdAt:
        dto.createdAt,

      updatedAt:
        dto.updatedAt
    };
  }

  private mapStandardPolicy(
    dto: StandardPolicyDto
  ): StandardPolicy {

    return {
      id: dto.id,
      name: dto.name,
      policyType: dto.policyType,
      description: dto.description,
      suggestedCoverageAmount:
        dto.suggestedCoverageAmount,
      suggestedPremiumAmount:
        dto.suggestedPremiumAmount,
      coverageDetails:
        dto.coverageDetails,
      recommendedBusinessType:
        dto.recommendedBusinessType
    };
  }

  private mapPayment(
    dto: PaymentResponseDto
  ): PaymentResponse {

    return {
      id: dto.id,
      policyId: dto.policyId,
      ownerId: dto.ownerId,
      amount: dto.amount,
      paymentDate: dto.paymentDate,
      status: dto.status,
      transactionReference:
        dto.transactionReference,
      createdAt:
        dto.createdAt
    };
  }
}