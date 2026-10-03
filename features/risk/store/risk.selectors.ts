import {
  createFeatureSelector,
  createSelector
} from '@ngrx/store';

import { RiskState } from './risk.reducer';
import { MitigationStatus } from '../models/risk.models';

export const selectRiskState =
  createFeatureSelector<RiskState>('risk');

export const selectAssessments = createSelector(
  selectRiskState,
  state => state.assessments
);

export const selectSimulations = createSelector(
  selectRiskState,
  state => state.simulations
);

export const selectMitigations = createSelector(
  selectRiskState,
  state => state.mitigations
);

export const selectRiskLoading = createSelector(
  selectRiskState,
  state => state.loading
);

export const selectRiskError = createSelector(
  selectRiskState,
  state => state.error
);

export const selectRiskSuccess = createSelector(
  selectRiskState,
  state => state.successMessage
);

export const selectMitigationsByAssessment = (
  assessmentId: number
) => createSelector(
  selectMitigations,
  mitigations =>
    mitigations.filter(
      item => item.assessmentId === assessmentId
    )
);

export const selectMitigationsByBusiness = (
  businessId: number
) => createSelector(
  selectMitigations,
  mitigations =>
    mitigations.filter(
      item => item.businessId === businessId
    )
);

export const selectMitigationsByStatus = (
  status: MitigationStatus
) => createSelector(
  selectMitigations,
  mitigations =>
    mitigations.filter(item => item.status === status)
);