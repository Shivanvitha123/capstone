/**
 * Risk assessment response DTO.
 */
export interface RiskAssessmentDto {
  id: number;
  businessId: number;
  policyId: number | null;
  riskScore: number;
  riskLevel: string;
  riskFactors: string | null;
  recommendation: string | null;
  assessedBy: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Risk assessment request DTO.
 */
export interface RiskAssessmentRequestDto {
  businessId: number;
  policyId?: number | null;
  industry: string;
  annualRevenue: number;
  employeeCount: number;
  branchCount: number;
  premisesType?: string | null;
  previousInsuranceClaims: number;
  existingInsurance?: boolean | null;
}

/**
 * Risk simulation response DTO.
 */
export interface SimulationDto {
  id: number;
  businessId: number;
  policyId: number | null;
  scenarioName: string;
  scenarioInput: string | null;
  projectedRiskScore: number | null;
  projectedRiskLevel: string | null;
  resultSummary: string | null;
  status: string;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Risk simulation request DTO.
 */
export interface SimulationRequestDto {
  businessId: number;
  policyId?: number | null;
  scenarioName: string;
  scenarioInput?: string;
}

/**
 * Risk mitigation priority.
 */
export type MitigationPriorityDto =
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'
  | 'CRITICAL';

/**
 * Risk mitigation status.
 */
export type MitigationStatusDto =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

/**
 * Risk mitigation response DTO.
 */
export interface RiskMitigationDto {
  id: number;
  assessmentId: number;
  businessId: number;
  ownerId: number;
  mitigationTitle: string;
  description: string | null;
  priority: MitigationPriorityDto;
  status: MitigationStatusDto;
  assignedTo: number | null;
  targetDate: string | null;
  completedAt: string | null;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Risk mitigation creation DTO.
 */
export interface RiskMitigationRequestDto {
  assessmentId: number;
  mitigationTitle: string;
  description?: string | null;
  priority: MitigationPriorityDto;
  assignedTo?: number | null;
  targetDate?: string | null;
}

/**
 * Risk mitigation status update DTO.
 */
export interface RiskMitigationStatusRequestDto {
  status: MitigationStatusDto;
}