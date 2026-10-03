import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  RiskAssessment,
  RiskAssessmentRequest,
  RiskMitigation,
  RiskMitigationRequest,
  Simulation,
  SimulationRequest
} from '../models/risk.models';

import { environment } from '../../../../environments/environments';

@Injectable({
  providedIn: 'root'
})
export class RiskApiService {

  private readonly http = inject(HttpClient);

  private readonly assessmentUrl =
    `${environment.apiUrl}/api/risk/assessments`;

  private readonly simulationUrl =
    `${environment.apiUrl}/api/risk/simulations`;

  private readonly mitigationUrl =
    `${environment.apiUrl}/api/risk/mitigations`;

  // =========================================================
  // RISK ASSESSMENTS
  // =========================================================

  createAssessment(
    request: RiskAssessmentRequest
  ): Observable<RiskAssessment> {

    return this.http.post<RiskAssessment>(
      this.assessmentUrl,
      request,
      {
        withCredentials: true
      }
    );
  }

  getAllAssessments(): Observable<RiskAssessment[]> {

    return this.http.get<RiskAssessment[]>(
      this.assessmentUrl,
      {
        withCredentials: true
      }
    );
  }

  getAssessment(
    id: number
  ): Observable<RiskAssessment> {

    return this.http.get<RiskAssessment>(
      `${this.assessmentUrl}/${id}`,
      {
        withCredentials: true
      }
    );
  }

  getByBusiness(
    businessId: number
  ): Observable<RiskAssessment[]> {

    return this.http.get<RiskAssessment[]>(
      `${this.assessmentUrl}/business/${businessId}`,
      {
        withCredentials: true
      }
    );
  }

  // =========================================================
  // RISK SIMULATIONS
  // =========================================================

  createSimulation(
    request: SimulationRequest
  ): Observable<Simulation> {

    return this.http.post<Simulation>(
      this.simulationUrl,
      request,
      {
        withCredentials: true
      }
    );
  }

  getSimulation(
    id: number
  ): Observable<Simulation> {

    return this.http.get<Simulation>(
      `${this.simulationUrl}/${id}`,
      {
        withCredentials: true
      }
    );
  }

  getSimulations(): Observable<Simulation[]> {

    return this.http.get<Simulation[]>(
      this.simulationUrl,
      {
        withCredentials: true
      }
    );
  }

  // =========================================================
  // RISK MITIGATIONS
  // =========================================================

  /**
   * Get all mitigation actions.
   */
  getMitigations(): Observable<RiskMitigation[]> {

    return this.http.get<RiskMitigation[]>(
      this.mitigationUrl,
      {
        withCredentials: true
      }
    );
  }

  /**
   * Get mitigation actions belonging
   * to a particular risk assessment.
   */
  getMitigationsByAssessment(
    assessmentId: number
  ): Observable<RiskMitigation[]> {

    return this.http.get<RiskMitigation[]>(
      `${this.mitigationUrl}/assessment/${assessmentId}`,
      {
        withCredentials: true
      }
    );
  }

  /**
   * Create a new risk mitigation action.
   */
  createMitigation(
    request: RiskMitigationRequest
  ): Observable<RiskMitigation> {

    return this.http.post<RiskMitigation>(
      this.mitigationUrl,
      request,
      {
        withCredentials: true
      }
    );
  }

  /**
   * Update the status of a mitigation action.
   */
  updateMitigationStatus(
    id: number,
    request: {
      status: string;
    }
  ): Observable<RiskMitigation> {

    return this.http.patch<RiskMitigation>(
      `${this.mitigationUrl}/${id}/status`,
      request,
      {
        withCredentials: true
      }
    );
  }
}