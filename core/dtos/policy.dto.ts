/**
 * Supported policy tenure units.
 *
 * These values match the backend API contract.
 */
export type TenureUnitDto = 'MONTHS' | 'YEARS';

/**
 * Supported policy payment frequencies.
 */
export type PaymentFrequencyDto = 'MONTHLY' | 'YEARLY';

/**
 * Policy lifecycle status.
 */
export type PolicyStatusDto =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'ACTIVE'
  | 'EXPIRED'
  | 'CANCELLATION_REQUESTED'
  | 'CANCELLED';

/**
 * Create policy request DTO.
 */
export interface CreatePolicyRequestDto {
  businessId: number;
  policyNumber: string;
  policyType: string;
  coverageAmount: number;
  coverageDetails?: string;
  coverageLocation?: string;
  premiumAmount: number;
  startDate: string;
  tenureValue: number;
  tenureUnit: TenureUnitDto;
  paymentFrequency: PaymentFrequencyDto;
  description?: string;
}

/**
 * Update policy request DTO.
 */
export interface UpdatePolicyRequestDto {
  businessId?: number;
  policyNumber?: string;
  policyType?: string;
  coverageAmount?: number;
  coverageDetails?: string;
  coverageLocation?: string;
  premiumAmount?: number;
  startDate?: string;
  tenureValue?: number;
  tenureUnit?: TenureUnitDto;
  paymentFrequency?: PaymentFrequencyDto;
  description?: string;
}

/**
 * Policy response DTO.
 */
export interface PolicyDto {
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
  tenureUnit: TenureUnitDto;
  paymentFrequency: PaymentFrequencyDto;
  status: PolicyStatusDto | string;
  cancellationReason?: string;
  previousStatus?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Standard/preloaded policy DTO.
 */
export interface StandardPolicyDto {
  id: string;
  name: string;
  policyType: string;
  description: string;
  suggestedCoverageAmount: number;
  suggestedPremiumAmount: number;
  coverageDetails: string;
  recommendedBusinessType: string;
}

/**
 * Policy cancellation request DTO.
 */
export interface CancellationRequestDto {
  reason: string;
}

/**
 * Policy cancellation review DTO.
 */
export interface CancellationReviewRequestDto {
  approved: boolean;
}

/**
 * Payment response DTO.
 */
export interface PaymentResponseDto {
  id: number;
  policyId: number;
  ownerId: number;
  amount: number;
  paymentDate: string;
  status: string;
  transactionReference: string;
  createdAt: string;
}