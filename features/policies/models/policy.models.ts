export type TenureUnit = 'MONTHS' | 'YEARS';

export type PaymentFrequency = 'MONTHLY' | 'YEARLY';

export type PolicyStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'ACTIVE'
  | 'EXPIRED'
  | 'CANCELLATION_REQUESTED'
  | 'CANCELLED';

export interface CreatePolicyRequest {
  businessId: number;
  policyNumber: string;
  policyType: string;
  coverageAmount: number;
  coverageDetails?: string;
  coverageLocation?: string;
  premiumAmount: number;
  startDate: string;
  tenureValue: number;
  tenureUnit: TenureUnit;
  paymentFrequency: PaymentFrequency;
  description?: string;
}

export interface UpdatePolicyRequest {
  businessId?: number;
  policyNumber?: string;
  policyType?: string;
  coverageAmount?: number;
  coverageDetails?: string;
  coverageLocation?: string;
  premiumAmount?: number;
  startDate?: string;
  tenureValue?: number;
  tenureUnit?: TenureUnit;
  paymentFrequency?: PaymentFrequency;
  description?: string;
}

export interface CancellationRequest {
  reason: string;
}

export interface CancellationReviewRequest {
  approved: boolean;
}

export interface Policy {
  id: number;
  businessId: number;
  ownerId: number;
  policyNumber: string;
  policyType: string;
  coverageAmount: number;
  coverageDetails?: string;
  coverageLocation?: string;
  premiumAmount: number;
  startDate: string;
  endDate: string;
  tenureValue: number;
  tenureUnit: TenureUnit;
  paymentFrequency: PaymentFrequency;
  status: PolicyStatus | string;
  cancellationReason?: string;
  previousStatus?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface StandardPolicy {
  id: string;
  name: string;
  policyType: string;
  description: string;
  suggestedCoverageAmount: number;
  suggestedPremiumAmount: number;
  coverageDetails: string;
  recommendedBusinessType: string;
}

export interface PaymentResponse {
  id: number;
  policyId: number;
  ownerId: number;
  amount: number;
  paymentDate: string;
  status: string;
  transactionReference: string;
  createdAt: string;
}