import { createReducer, on } from '@ngrx/store';

import * as RiskActions from './risk.actions';

import {
  RiskAssessment,
  Simulation,
  RiskMitigation
} from '../models/risk.models';

export interface RiskState {
  assessments: RiskAssessment[];
  simulations: Simulation[];
  mitigations: RiskMitigation[];
  loading: boolean;
  error: string | null;
  successMessage: string | null;
}

export const initialRiskState: RiskState = {
  assessments: [],
  simulations: [],
  mitigations: [],
  loading: false,
  error: null,
  successMessage: null
};

export const riskReducer = createReducer(
  initialRiskState,

  on(
    RiskActions.loadAssessments,
    RiskActions.loadAllAssessments,
    RiskActions.loadSimulations,
    RiskActions.createAssessment,
    RiskActions.createSimulation,
    RiskActions.loadMitigations,
    RiskActions.loadMitigationsByAssessment,
    RiskActions.createMitigation,
    RiskActions.updateMitigationStatus,
    state => ({
      ...state,
      loading: true,
      error: null
    })
  ),

  on(
    RiskActions.loadAssessmentsSuccess,
    (state, { assessments }) => ({
      ...state,
      assessments,
      loading: false
    })
  ),

  on(
    RiskActions.createAssessmentSuccess,
    (state, { assessment }) => ({
      ...state,
      assessments: [assessment, ...state.assessments],
      loading: false,
      successMessage: 'Risk assessment created successfully.'
    })
  ),

  on(
    RiskActions.loadSimulationsSuccess,
    (state, { simulations }) => ({
      ...state,
      simulations,
      loading: false
    })
  ),

  on(
    RiskActions.createSimulationSuccess,
    (state, { simulation }) => ({
      ...state,
      simulations: [simulation, ...state.simulations],
      loading: false,
      successMessage: 'Simulation created successfully.'
    })
  ),

  on(
    RiskActions.loadMitigationsSuccess,
    (state, { mitigations }) => ({
      ...state,
      mitigations,
      loading: false
    })
  ),

  on(
    RiskActions.loadMitigationsByAssessmentSuccess,
    (state, { mitigations }) => ({
      ...state,
      mitigations,
      loading: false
    })
  ),

  on(
    RiskActions.createMitigationSuccess,
    (state, { mitigation }) => ({
      ...state,
      mitigations: [
        mitigation,
        ...state.mitigations.filter(item => item.id !== mitigation.id)
      ],
      loading: false,
      successMessage: 'Mitigation created successfully.'
    })
  ),

  on(
    RiskActions.updateMitigationStatusSuccess,
    (state, { mitigation }) => ({
      ...state,
      mitigations: state.mitigations.map(item =>
        item.id === mitigation.id ? mitigation : item
      ),
      loading: false,
      successMessage: 'Mitigation status updated successfully.'
    })
  ),

  on(
    RiskActions.riskFailure,
    (state, { error }) => ({
      ...state,
      loading: false,
      error
    })
  ),

  on(
    RiskActions.clearRiskMessages,
    state => ({
      ...state,
      error: null,
      successMessage: null
    })
  )
);