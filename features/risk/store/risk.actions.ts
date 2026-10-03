import { createAction, props } from '@ngrx/store';

import {
  RiskAssessment,
  RiskAssessmentRequest,
  Simulation,
  SimulationRequest,
  RiskMitigation,
  RiskMitigationRequest,
  MitigationStatus
} from '../models/risk.models';

// Existing assessment actions

export const loadAssessments = createAction(
  '[Risk] Load Assessments',
  props<{ businessId: number }>()
);

export const loadAllAssessments = createAction(
  '[Risk] Load All Assessments'
);

export const loadAssessmentsSuccess = createAction(
  '[Risk] Load Assessments Success',
  props<{ assessments: RiskAssessment[] }>()
);

export const createAssessment = createAction(
  '[Risk] Create Assessment',
  props<{ request: RiskAssessmentRequest }>()
);

export const createAssessmentSuccess = createAction(
  '[Risk] Create Assessment Success',
  props<{ assessment: RiskAssessment }>()
);

// Existing simulation actions

export const loadSimulations = createAction(
  '[Risk] Load Simulations'
);

export const loadSimulationsSuccess = createAction(
  '[Risk] Load Simulations Success',
  props<{ simulations: Simulation[] }>()
);

export const createSimulation = createAction(
  '[Risk] Create Simulation',
  props<{ request: SimulationRequest }>()
);

export const createSimulationSuccess = createAction(
  '[Risk] Create Simulation Success',
  props<{ simulation: Simulation }>()
);

// Mitigation actions

export const loadMitigations = createAction(
  '[Risk] Load Mitigations'
);

export const loadMitigationsSuccess = createAction(
  '[Risk] Load Mitigations Success',
  props<{ mitigations: RiskMitigation[] }>()
);

export const loadMitigationsByAssessment = createAction(
  '[Risk] Load Mitigations By Assessment',
  props<{ assessmentId: number }>()
);

export const loadMitigationsByAssessmentSuccess = createAction(
  '[Risk] Load Mitigations By Assessment Success',
  props<{ mitigations: RiskMitigation[] }>()
);

export const createMitigation = createAction(
  '[Risk] Create Mitigation',
  props<{ request: RiskMitigationRequest }>()
);

export const createMitigationSuccess = createAction(
  '[Risk] Create Mitigation Success',
  props<{ mitigation: RiskMitigation }>()
);

export const updateMitigationStatus = createAction(
  '[Risk] Update Mitigation Status',
  props<{
    id: number;
    status: MitigationStatus;
  }>()
);

export const updateMitigationStatusSuccess = createAction(
  '[Risk] Update Mitigation Status Success',
  props<{ mitigation: RiskMitigation }>()
);

// Shared messages

export const riskFailure = createAction(
  '[Risk] Failure',
  props<{ error: string }>()
);

export const clearRiskMessages = createAction(
  '[Risk] Clear Messages'
);