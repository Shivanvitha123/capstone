/**
 * Claim lifecycle status.
 */
export type ClaimStatusDto =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'SETTLED';

/**
 * Supported claim types.
 */
export type ClaimTypeDto =
  | 'PROPERTY_DAMAGE'
  | 'FIRE'
  | 'FLOOD'
  | 'THEFT'
  | 'LIABILITY'
  | 'ACCIDENT'
  | 'OTHER';

/**
 * Claim response DTO.
 */
export interface ClaimDto {
  id: number;
  businessId: number;
  policyId: number;
  ownerId: number;
  claimNumber: string;
  claimType: ClaimTypeDto;
  incidentDate: string;
  reportedDate: string;
  claimedAmount: number;
  approvedAmount: number | null;
  description: string | null;
  status: ClaimStatusDto;
  assignedAdjusterId: number | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Claim creation request DTO.
 */
export interface CreateClaimRequestDto {
  businessId: number;
  policyId: number;
  claimNumber: string;
  claimType: ClaimTypeDto;
  incidentDate: string;
  reportedDate: string;
  claimedAmount: number;
  description?: string;
}

/**
 * Claim status update DTO.
 */
export interface UpdateClaimStatusRequestDto {
  status: ClaimStatusDto;
  approvedAmount?: number;
  assignedAdjusterId?: number;
}

/**
 * Recovery response DTO.
 */
export interface RecoveryDto {
  id: number;
  claimId: number;
  ownerId: number;
  recoveryAmount: number;
  recoverySource: string | null;
  description: string | null;
  status: string;
  processedBy: number | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Recovery creation DTO.
 */
export interface CreateRecoveryRequestDto {
  recoveryAmount: number;
  recoverySource?: string;
  description?: string;
}

/**
 * Recovery status update DTO.
 */
export interface UpdateRecoveryStatusRequestDto {
  status: string;
}