export interface BusinessRequest {
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

  // Additional business information
  businessDescription?: string | null;
  website?: string | null;
  branchCount?: number | null;
  premisesType?: string | null;
  previousInsuranceClaims?: number | null;
  existingInsurance?: boolean | null;
}

export interface Business extends BusinessRequest {
  id: number;
  ownerId: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;

  // Additional business information returned by the backend
  businessDescription?: string | null;
  website?: string | null;
  branchCount?: number | null;
  premisesType?: string | null;
  previousInsuranceClaims?: number | null;
  existingInsurance?: boolean | null;
}

export interface BusinessSummary {
  id: number;
  ownerId: number;
  businessName: string;
  industry: string;
  status: string;
}