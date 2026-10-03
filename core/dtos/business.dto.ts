/**
 * Business creation request DTO.
 */
export interface CreateBusinessRequestDto {
  businessName: string;
  registrationNumber: string;
  businessType: string;
  industry: string;
  address: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  contactEmail: string;
  contactPhone: string;
  annualRevenue: number;
  employeeCount: number;
  establishedDate: string;

  businessDescription?: string | null;
  website?: string | null;
  branchCount?: number | null;
  premisesType?: string | null;
  previousInsuranceClaims?: number | null;
  existingInsurance?: boolean | null;
}

/**
 * Business response DTO.
 */
export interface BusinessDto extends CreateBusinessRequestDto {
  id: number;
  ownerId: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Business summary DTO.
 * Used when only essential business information is required.
 */
export interface BusinessSummaryDto {
  id: number;
  ownerId: number;
  businessName: string;
  industry: string;
  status: string;
}

/**
 * Business update request DTO.
 */
export interface UpdateBusinessRequestDto {
  businessName?: string;
  registrationNumber?: string;
  businessType?: string;
  industry?: string;
  address?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  contactEmail?: string;
  contactPhone?: string;
  annualRevenue?: number;
  employeeCount?: number;
  establishedDate?: string;

  businessDescription?: string | null;
  website?: string | null;
  branchCount?: number | null;
  premisesType?: string | null;
  previousInsuranceClaims?: number | null;
  existingInsurance?: boolean | null;
}

/**
 * Business deletion request DTO.
 */
export interface BusinessDeletionRequestDto {
  id: number;
  businessId: number;
  ownerId: number;
  reviewerId: number | null;
  reason: string;
  status: string;
  createdAt: string;
  reviewedAt: string | null;
}

/**
 * Business deletion creation DTO.
 */
export interface CreateBusinessDeletionRequestDto {
  reason: string;
}