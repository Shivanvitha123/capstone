export interface RiskAssessment {
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

export interface RiskAssessmentRequest {
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

export interface Simulation {
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

export interface SimulationRequest {
  businessId: number;
  policyId?: number | null;
  scenarioName: string;
  scenarioInput?: string;
}

export type MitigationPriority =
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'
  | 'CRITICAL';

export type MitigationStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface RiskMitigation {
  id: number;
  assessmentId: number;
  businessId: number;
  ownerId: number;
  mitigationTitle: string;
  description: string | null;
  priority: MitigationPriority;
  status: MitigationStatus;
  assignedTo: number | null;
  targetDate: string | null;
  completedAt: string | null;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

export interface RiskMitigationRequest {
  assessmentId: number;
  mitigationTitle: string;
  description?: string | null;
  priority: MitigationPriority;
  assignedTo?: number | null;
  targetDate?: string | null;
}

export interface RiskMitigationStatusRequest {
  status: MitigationStatus;
}