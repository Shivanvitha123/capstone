import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject
} from '@angular/core';

import {
  CommonModule,
  DatePipe
} from '@angular/common';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { Store } from '@ngrx/store';
import { filter, forkJoin, take } from 'rxjs';

import * as RiskActions
  from '../../store/risk.actions';

import {
  selectAssessments,
  selectSimulations,
  selectMitigations,
  selectRiskError,
  selectRiskLoading,
  selectRiskSuccess
} from '../../store/risk.selectors';

import {
  MitigationPriority,
  MitigationStatus,
  RiskMitigation
} from '../../models/risk.models';

import { selectAuth } from '../../../auth/store/auth.selector';

import { BusinessApiService } from '../../../business/services/business-api.service';

import {
  Business,
  BusinessSummary
} from '../../../business/model/business.model';


@Component({
  selector: 'app-risk',
  standalone: true,

  imports: [
    CommonModule,
    ReactiveFormsModule,
    DatePipe
  ],

  templateUrl: './risk.component.html',
  styleUrl: './risk.component.css',

  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RiskComponent implements OnInit {

  private readonly store = inject(Store);

  private readonly fb = inject(FormBuilder);

  private readonly businessApi =
    inject(BusinessApiService);


  // =========================================================
  // CURRENT USER / RBAC
  // =========================================================

  userRole = '';

  canManageAssessments = false;

  readonly canViewAssessments = true;

  canViewSimulations = false;

  canManageSimulations = false;

  canCreateMitigations = false;

  canUpdateMitigations = false;


  // =========================================================
  // BUSINESS DATA
  // =========================================================

  businesses: Business[] = [];


  // =========================================================
  // STORE SELECTORS
  // =========================================================

  readonly assessments$ =
    this.store.select(selectAssessments);

  readonly simulations$ =
    this.store.select(selectSimulations);

  readonly mitigations$ =
    this.store.select(selectMitigations);

  readonly loading$ =
    this.store.select(selectRiskLoading);

  readonly error$ =
    this.store.select(selectRiskError);

  readonly success$ =
    this.store.select(selectRiskSuccess);


  // =========================================================
  // ACTIVE TAB
  // =========================================================

  activeTab:
    'assessments' |
    'simulations' |
    'mitigations' =
    'assessments';


  // =========================================================
  // FORM VISIBILITY
  // =========================================================

  showAssessmentForm = false;

  showSimulationForm = false;

  showMitigationForm = false;


  // =========================================================
  // ASSESSMENT FORM
  // =========================================================

  readonly assessmentForm =
    this.fb.nonNullable.group({

      businessId: [
        0,
        [
          Validators.required,
          Validators.min(1)
        ]
      ],

      policyId: [0],

      industry: [
        '',
        Validators.required
      ],

      annualRevenue: [
        0,
        [
          Validators.required,
          Validators.min(0)
        ]
      ],

      employeeCount: [
        0,
        [
          Validators.required,
          Validators.min(0)
        ]
      ],

      branchCount: [
        0,
        [
          Validators.required,
          Validators.min(0)
        ]
      ],

      premisesType: [''],

      previousInsuranceClaims: [
        0,
        [
          Validators.required,
          Validators.min(0)
        ]
      ],

      existingInsurance: [false]

    });


  // =========================================================
  // SIMULATION FORM
  // =========================================================

  readonly simulationForm =
    this.fb.nonNullable.group({

      businessId: [
        0,
        [
          Validators.required,
          Validators.min(1)
        ]
      ],

      policyId: [0],

      scenarioName: [
        '',
        Validators.required
      ],

      scenarioInput: ['']

    });


  // =========================================================
  // MITIGATION FORM
  // =========================================================

  readonly mitigationForm =
    this.fb.nonNullable.group({

      assessmentId: [
        0,
        [
          Validators.required,
          Validators.min(1)
        ]
      ],

      mitigationTitle: [
        '',
        [
          Validators.required,
          Validators.maxLength(255)
        ]
      ],

      description: [
        '',
        Validators.maxLength(2000)
      ],

      priority: [
        'MEDIUM' as MitigationPriority,
        Validators.required
      ],

      assignedTo: [0],

      targetDate: ['']

    });


  // =========================================================
  // PRIORITIES
  // =========================================================

  readonly priorities: MitigationPriority[] = [
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
  ];


  // =========================================================
  // INITIALIZATION
  // =========================================================

  ngOnInit(): void {
    this.initializeUser();
  }


  // =========================================================
  // INITIALIZE CURRENT USER
  // =========================================================

  private initializeUser(): void {

    this.store.select(selectAuth).pipe(
      filter(state => state.initialized),
      take(1)
    ).subscribe(state => {

      this.userRole =
        String(state.user?.role ?? '')
          .replace(/^ROLE_/i, '')
          .toUpperCase();


      // -------------------------------------------------------
      // ASSESSMENT PERMISSIONS
      // -------------------------------------------------------

      this.canManageAssessments =
        this.userRole === 'RISK_ENGINEER';


      // -------------------------------------------------------
      // SIMULATION PERMISSIONS
      // -------------------------------------------------------

      this.canViewSimulations =
        [
          'ADMIN',
          'UNDERWRITER',
          'RISK_ENGINEER'
        ].includes(this.userRole);

      this.canManageSimulations =
        [
          'ADMIN',
          'RISK_ENGINEER'
        ].includes(this.userRole);


      // -------------------------------------------------------
      // MITIGATION PERMISSIONS
      // -------------------------------------------------------

      this.canCreateMitigations =
        this.userRole === 'RISK_ENGINEER';

      this.canUpdateMitigations =
        [
          'ADMIN',
          'UNDERWRITER',
          'RISK_ENGINEER'
        ].includes(this.userRole);


      // -------------------------------------------------------
      // IMPORTANT:
      // Only load businesses AFTER the role is known.
      // This prevents ADMIN from calling /api/business/me.
      // -------------------------------------------------------

      this.loadBusinesses();

      this.loadRiskData();

    });
  }


  // =========================================================
  // LOAD BUSINESSES
  // =========================================================
  //
  // BUSINESS_OWNER:
  //     GET /api/business/me
  //
  // ADMIN / UNDERWRITER / RISK_ENGINEER:
  //     GET /api/business
  //     followed by GET /api/business/{id}
  //
  // This prevents the ADMIN 403 caused by /business/me.
  // =========================================================

  private loadBusinesses(): void {

    // -------------------------------------------------------
    // BUSINESS OWNER
    // -------------------------------------------------------

    if (this.userRole === 'BUSINESS_OWNER') {

      this.businessApi
        .getMyBusinesses()
        .subscribe({

          next: businesses => {

            this.businesses =
              businesses ?? [];

          },

          error: error => {

            console.error(
              'Failed to load own businesses:',
              error
            );

          }

        });

      return;
    }


    // -------------------------------------------------------
    // PRIVILEGED ROLES
    // -------------------------------------------------------

    const canViewAllBusinesses =
      [
        'ADMIN',
        'UNDERWRITER',
        'RISK_ENGINEER'
      ].includes(this.userRole);


    if (!canViewAllBusinesses) {

      this.businesses = [];

      return;
    }


    // -------------------------------------------------------
    // STEP 1:
    // Get business summaries.
    //
    // GET /api/business
    // -------------------------------------------------------

    this.businessApi
      .getAll()
      .subscribe({

        next: (
          summaries: BusinessSummary[]
        ) => {

          if (
            !summaries ||
            summaries.length === 0
          ) {

            this.businesses = [];

            return;
          }


          // -------------------------------------------------
          // STEP 2:
          // Get complete business information.
          //
          // GET /api/business/{id}
          //
          // Risk assessment needs:
          // - industry
          // - annualRevenue
          // - employeeCount
          // - branchCount
          // - premisesType
          // - previousInsuranceClaims
          // - existingInsurance
          // -------------------------------------------------

          forkJoin(
            summaries.map(summary =>
              this.businessApi.getById(
                summary.id
              )
            )
          ).subscribe({

            next: businesses => {

              this.businesses =
                businesses ?? [];

            },

            error: error => {

              console.error(
                'Failed to load business details:',
                error
              );

              this.businesses = [];

            }

          });

        },

        error: error => {

          console.error(
            'Failed to load businesses:',
            error
          );

          this.businesses = [];

        }

      });

  }


  // =========================================================
  // LOAD RISK DATA
  // =========================================================

  private loadRiskData(): void {

    this.store.dispatch(
      RiskActions.clearRiskMessages()
    );


    // -------------------------------------------------------
    // LOAD ALL ASSESSMENTS
    // -------------------------------------------------------

    this.store.dispatch(
      RiskActions.loadAllAssessments()
    );


    // -------------------------------------------------------
    // LOAD SIMULATIONS
    // -------------------------------------------------------

    if (this.canViewSimulations) {

      this.store.dispatch(
        RiskActions.loadSimulations()
      );

    }


    // -------------------------------------------------------
    // LOAD MITIGATIONS
    // -------------------------------------------------------

    this.store.dispatch(
      RiskActions.loadMitigations()
    );

  }


  // =========================================================
  // TAB MANAGEMENT
  // =========================================================

  setTab(
    tab:
      'assessments' |
      'simulations' |
      'mitigations'
  ): void {

    if (
      tab === 'simulations' &&
      !this.canViewSimulations
    ) {

      return;
    }


    this.activeTab = tab;


    if (tab === 'mitigations') {

      this.store.dispatch(
        RiskActions.loadMitigations()
      );

    }

  }


  // =========================================================
  // ASSESSMENT FORM
  // =========================================================

  openAssessmentForm(): void {

    if (!this.canManageAssessments) {
      return;
    }


    this.assessmentForm.reset({

      businessId: 0,

      policyId: 0,

      industry: '',

      annualRevenue: 0,

      employeeCount: 0,

      branchCount: 0,

      premisesType: '',

      previousInsuranceClaims: 0,

      existingInsurance: false

    });


    this.showAssessmentForm = true;

  }


  closeAssessmentForm(): void {

    this.showAssessmentForm = false;

  }


  // =========================================================
  // BUSINESS SELECTION
  // =========================================================

  onBusinessSelected(): void {

    const businessId =
      this.assessmentForm
        .controls
        .businessId
        .value;


    const business =
      this.businesses.find(
        item =>
          item.id === Number(businessId)
      );


    if (!business) {
      return;
    }


    // -------------------------------------------------------
    // Automatically populate risk factors
    // from the selected business.
    // -------------------------------------------------------

    this.assessmentForm.patchValue({

      industry:
        business.industry ?? '',

      annualRevenue:
        business.annualRevenue ?? 0,

      employeeCount:
        business.employeeCount ?? 0,

      branchCount:
        business.branchCount ?? 0,

      premisesType:
        business.premisesType ?? '',

      previousInsuranceClaims:
        business.previousInsuranceClaims ?? 0,

      existingInsurance:
        business.existingInsurance ?? false

    });

  }


  // =========================================================
  // CREATE ASSESSMENT
  // =========================================================

  createAssessment(): void {

    if (!this.canManageAssessments) {
      return;
    }


    if (this.assessmentForm.invalid) {

      this.assessmentForm.markAllAsTouched();

      return;
    }


    const value =
      this.assessmentForm.getRawValue();


    this.store.dispatch(

      RiskActions.createAssessment({

        request: {

          businessId:
            value.businessId,

          policyId:
            value.policyId || null,

          industry:
            value.industry.trim(),

          annualRevenue:
            value.annualRevenue,

          employeeCount:
            value.employeeCount,

          branchCount:
            value.branchCount,

          premisesType:
            value.premisesType.trim() || null,

          previousInsuranceClaims:
            value.previousInsuranceClaims,

          existingInsurance:
            value.existingInsurance

        }

      })

    );


    this.showAssessmentForm = false;

  }


  // =========================================================
  // LOAD BUSINESS ASSESSMENTS
  // =========================================================

  loadBusinessAssessments(
    businessId: number
  ): void {

    if (!businessId) {
      return;
    }


    this.store.dispatch(

      RiskActions.loadAssessments({
        businessId
      })

    );

  }


  // =========================================================
  // SIMULATION FORM
  // =========================================================

  openSimulationForm(): void {

    if (!this.canManageSimulations) {
      return;
    }


    this.simulationForm.reset({

      businessId: 0,

      policyId: 0,

      scenarioName: '',

      scenarioInput: ''

    });


    this.showSimulationForm = true;

  }


  closeSimulationForm(): void {

    this.showSimulationForm = false;

  }


  // =========================================================
  // CREATE SIMULATION
  // =========================================================

  createSimulation(): void {

    if (!this.canManageSimulations) {
      return;
    }


    if (this.simulationForm.invalid) {

      this.simulationForm.markAllAsTouched();

      return;
    }


    const value =
      this.simulationForm.getRawValue();


    this.store.dispatch(

      RiskActions.createSimulation({

        request: {

          businessId:
            value.businessId,

          policyId:
            value.policyId || null,

          scenarioName:
            value.scenarioName.trim(),

          scenarioInput:
            value.scenarioInput || ''

        }

      })

    );


    this.showSimulationForm = false;

  }


  // =========================================================
  // MITIGATION FORM
  // =========================================================

  openMitigationForm(): void {

    if (!this.canCreateMitigations) {
      return;
    }


    this.mitigationForm.reset({

      assessmentId: 0,

      mitigationTitle: '',

      description: '',

      priority: 'MEDIUM',

      assignedTo: 0,

      targetDate: ''

    });


    this.showMitigationForm = true;

  }


  closeMitigationForm(): void {

    this.showMitigationForm = false;

  }


  // =========================================================
  // CREATE MITIGATION
  // =========================================================

  createMitigation(): void {

    if (!this.canCreateMitigations) {
      return;
    }


    if (this.mitigationForm.invalid) {

      this.mitigationForm.markAllAsTouched();

      return;
    }


    const value =
      this.mitigationForm.getRawValue();


    this.store.dispatch(

      RiskActions.createMitigation({

        request: {

          assessmentId:
            value.assessmentId,

          mitigationTitle:
            value.mitigationTitle.trim(),

          description:
            value.description.trim() || null,

          priority:
            value.priority,

          assignedTo:
            value.assignedTo || null,

          targetDate:
            value.targetDate || null

        }

      })

    );


    this.showMitigationForm = false;

  }


  // =========================================================
  // UPDATE MITIGATION STATUS
  // =========================================================

  updateMitigationStatus(
    mitigation: RiskMitigation,
    status: MitigationStatus
  ): void {

    if (!this.canUpdateMitigations) {
      return;
    }


    if (
      !this.canTransition(
        mitigation.status,
        status
      )
    ) {

      return;
    }


    this.store.dispatch(

      RiskActions.updateMitigationStatus({

        id: mitigation.id,

        status

      })

    );

  }


  // =========================================================
  // MITIGATION STATUS TRANSITIONS
  // =========================================================

  canTransition(
    current: MitigationStatus,
    next: MitigationStatus
  ): boolean {

    if (current === next) {
      return true;
    }


    switch (current) {

      case 'OPEN':

        return [
          'IN_PROGRESS',
          'CANCELLED'
        ].includes(next);


      case 'IN_PROGRESS':

        return [
          'OPEN',
          'COMPLETED',
          'CANCELLED'
        ].includes(next);


      case 'COMPLETED':

      case 'CANCELLED':

        return false;


      default:

        return false;

    }

  }


  // =========================================================
  // AVAILABLE MITIGATION STATUSES
  // =========================================================

  availableStatuses(
    current: MitigationStatus
  ): MitigationStatus[] {

    switch (current) {

      case 'OPEN':

        return [
          'OPEN',
          'IN_PROGRESS',
          'CANCELLED'
        ];


      case 'IN_PROGRESS':

        return [
          'IN_PROGRESS',
          'OPEN',
          'COMPLETED',
          'CANCELLED'
        ];


      case 'COMPLETED':

        return [
          'COMPLETED'
        ];


      case 'CANCELLED':

        return [
          'CANCELLED'
        ];


      default:

        return [];

    }

  }


  // =========================================================
  // CSS HELPERS
  // =========================================================

  mitigationStatusClass(
    status: string
  ): string {

    return status
      .toLowerCase()
      .replace('_', '-');

  }


  priorityClass(
    priority: string
  ): string {

    return priority.toLowerCase();

  }


  riskClass(
    level: string | null
  ): string {

    return level
      ? level.toLowerCase()
      : 'unknown';

  }

}