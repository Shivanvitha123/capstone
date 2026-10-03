import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, mergeMap, of } from 'rxjs';

import * as RiskActions from './risk.actions';
import { RiskApiService } from '../services/risk-api.service';

@Injectable()
export class RiskEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(RiskApiService);

  loadAssessments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.loadAssessments),
      mergeMap(({ businessId }) =>
        this.api.getByBusiness(businessId).pipe(
          map(assessments =>
            RiskActions.loadAssessmentsSuccess({ assessments })
          ),
          catchError(error =>
            of(RiskActions.riskFailure({
              error: this.errorMessage(
                error,
                'Failed to load business assessments.'
              )
            }))
          )
        )
      )
    )
  );

  loadAllAssessments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.loadAllAssessments),
      mergeMap(() =>
        this.api.getAllAssessments().pipe(
          map(assessments =>
            RiskActions.loadAssessmentsSuccess({ assessments })
          ),
          catchError(error =>
            of(RiskActions.riskFailure({
              error: this.errorMessage(
                error,
                'Failed to load risk assessments.'
              )
            }))
          )
        )
      )
    )
  );

  createAssessment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.createAssessment),
      mergeMap(({ request }) =>
        this.api.createAssessment(request).pipe(
          map(assessment =>
            RiskActions.createAssessmentSuccess({ assessment })
          ),
          catchError(error =>
            of(RiskActions.riskFailure({
              error: this.errorMessage(
                error,
                'Failed to create risk assessment.'
              )
            }))
          )
        )
      )
    )
  );

  reloadAssessmentsAfterCreate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.createAssessmentSuccess),
      map(() => RiskActions.loadAllAssessments())
    )
  );

  loadSimulations$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.loadSimulations),
      mergeMap(() =>
        this.api.getSimulations().pipe(
          map(simulations =>
            RiskActions.loadSimulationsSuccess({ simulations })
          ),
          catchError(error =>
            of(RiskActions.riskFailure({
              error: this.errorMessage(
                error,
                'Failed to load simulations.'
              )
            }))
          )
        )
      )
    )
  );

  createSimulation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.createSimulation),
      mergeMap(({ request }) =>
        this.api.createSimulation(request).pipe(
          map(simulation =>
            RiskActions.createSimulationSuccess({ simulation })
          ),
          catchError(error =>
            of(RiskActions.riskFailure({
              error: this.errorMessage(
                error,
                'Failed to create simulation.'
              )
            }))
          )
        )
      )
    )
  );

  reloadSimulationsAfterCreate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.createSimulationSuccess),
      map(() => RiskActions.loadSimulations())
    )
  );

  // Mitigation effects

  loadMitigations$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.loadMitigations),
      mergeMap(() =>
        this.api.getMitigations().pipe(
          map(mitigations =>
            RiskActions.loadMitigationsSuccess({ mitigations })
          ),
          catchError(error =>
            of(RiskActions.riskFailure({
              error: this.errorMessage(
                error,
                'Failed to load mitigations.'
              )
            }))
          )
        )
      )
    )
  );

  loadMitigationsByAssessment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.loadMitigationsByAssessment),
      mergeMap(({ assessmentId }) =>
        this.api.getMitigationsByAssessment(assessmentId).pipe(
          map(mitigations =>
            RiskActions.loadMitigationsByAssessmentSuccess({
              mitigations
            })
          ),
          catchError(error =>
            of(RiskActions.riskFailure({
              error: this.errorMessage(
                error,
                'Failed to load assessment mitigations.'
              )
            }))
          )
        )
      )
    )
  );

  createMitigation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.createMitigation),
      mergeMap(({ request }) =>
        this.api.createMitigation(request).pipe(
          map(mitigation =>
            RiskActions.createMitigationSuccess({ mitigation })
          ),
          catchError(error =>
            of(RiskActions.riskFailure({
              error: this.errorMessage(
                error,
                'Failed to create mitigation.'
              )
            }))
          )
        )
      )
    )
  );

  reloadMitigationsAfterCreate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.createMitigationSuccess),
      map(() => RiskActions.loadMitigations())
    )
  );

  updateMitigationStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.updateMitigationStatus),
      mergeMap(({ id, status }) =>
        this.api.updateMitigationStatus(id, { status }).pipe(
          map(mitigation =>
            RiskActions.updateMitigationStatusSuccess({ mitigation })
          ),
          catchError(error =>
            of(RiskActions.riskFailure({
              error: this.errorMessage(
                error,
                'Failed to update mitigation status.'
              )
            }))
          )
        )
      )
    )
  );

  reloadMitigationsAfterStatusUpdate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.updateMitigationStatusSuccess),
      map(() => RiskActions.loadMitigations())
    )
  );

  private errorMessage(error: any, fallback: string): string {
    return error?.error?.message
      || error?.error?.detail
      || error?.message
      || fallback;
  }
}