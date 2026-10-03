import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject
} from '@angular/core';

import {
  CommonModule,
  CurrencyPipe,
  DatePipe
} from '@angular/common';

import {
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { Store } from '@ngrx/store';
import { take } from 'rxjs';

import { StatusCountPipe } from '../../../../shared/pipes/status-count.pipe';

import * as PolicyActions from '../../store/policy.actions';

import {
  selectPolicies,
  selectPolicyError,
  selectPolicyLoading,
  selectPolicySuccessMessage
} from '../../store/policy.selectors';

import { selectRole } from '../../../auth/store/auth.selector';

import {
  Policy,
  CreatePolicyRequest,
  UpdatePolicyRequest,
  TenureUnit,
  PaymentFrequency,
  StandardPolicy,
  PaymentResponse
} from '../../models/policy.models';

import { PolicyApiService } from '../../services/policy-api.service';

@Component({
  selector: 'app-policies',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    CurrencyPipe,
    DatePipe,
    StatusCountPipe
  ],
  templateUrl: './policies.component.html',
  styleUrl: './policies.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PoliciesComponent implements OnInit {

  private readonly store = inject(Store);
  private readonly fb = inject(FormBuilder);
  private readonly policyApi = inject(PolicyApiService);

  // ==========================================
  // STORE SELECTORS
  // ==========================================

  readonly policies$ = this.store.select(selectPolicies);
  readonly loading$ = this.store.select(selectPolicyLoading);
  readonly error$ = this.store.select(selectPolicyError);
  readonly successMessage$ = this.store.select(
    selectPolicySuccessMessage
  );

  // ==========================================
  // POLICY CONFIGURATION
  // ==========================================

  readonly statuses = [
    'DRAFT',
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED',
    'ACTIVE',
    'EXPIRED'
  ];

  readonly tenureUnits: TenureUnit[] = [
    'MONTHS',
    'YEARS'
  ];

  readonly paymentFrequencies: PaymentFrequency[] = [
    'MONTHLY',
    'YEARLY'
  ];

  // ==========================================
  // POLICY FORM
  // ==========================================

  readonly policyForm = this.fb.nonNullable.group({
    businessId: [
      0,
      [
        Validators.required,
        Validators.min(1)
      ]
    ],

    policyNumber: [
      '',
      Validators.required
    ],

    policyType: [
      '',
      Validators.required
    ],

    coverageAmount: [
      0,
      [
        Validators.required,
        Validators.min(0.01)
      ]
    ],

    coverageDetails: [
      '',
      Validators.maxLength(1000)
    ],

    coverageLocation: [
      '',
      Validators.maxLength(255)
    ],

    premiumAmount: [
      0,
      [
        Validators.required,
        Validators.min(0.01)
      ]
    ],

    startDate: [
      '',
      Validators.required
    ],

    tenureValue: [
      1,
      [
        Validators.required,
        Validators.min(1),
        Validators.max(120)
      ]
    ],

    tenureUnit: [
      'YEARS' as TenureUnit,
      Validators.required
    ],

    paymentFrequency: [
      'MONTHLY' as PaymentFrequency,
      Validators.required
    ],

    description: ['']
  });

  // ==========================================
  // CANCELLATION FORM
  // ==========================================

  readonly cancellationForm = this.fb.nonNullable.group({
    reason: [
      '',
      [
        Validators.required,
        Validators.maxLength(1000)
      ]
    ]
  });

  // ==========================================
  // COMPONENT STATE
  // ==========================================

  showForm = false;
  saving = false;

  standardPolicies: StandardPolicy[] = [];
  standardPoliciesLoading = false;
  standardPoliciesError = '';
  selectedStandardPolicyId = '';

  editingPolicy: Policy | null = null;
  selectedPolicy: Policy | null = null;

  currentRole = '';

  // ==========================================
  // CANCELLATION STATE
  // ==========================================

  cancellationPolicy: Policy | null = null;
  cancellationBusy = false;
  cancellationError = '';
  cancellationSuccess = '';

  /**
   * Controls the cancellation modal in the template.
   */
  get showCancellationModal(): boolean {
    return this.cancellationPolicy !== null;
  }

  // ==========================================
  // PAYMENT STATE
  // ==========================================

  paymentBusyPolicyId: number | null = null;
  paymentError = '';
  paymentSuccess = '';

  lastPayment: PaymentResponse | null = null;

  // ==========================================
  // INITIALIZATION
  // ==========================================

  ngOnInit(): void {
    this.store.select(selectRole)
      .pipe(take(1))
      .subscribe(role => {
        this.currentRole = role ?? '';
        this.loadPolicies();
      });
  }

  // ==========================================
  // LOAD POLICIES
  // ==========================================

  private loadPolicies(): void {
    if (this.currentRole === 'BUSINESS_OWNER') {
      this.store.dispatch(
        PolicyActions.loadOwnerPolicies()
      );
    } else {
      this.store.dispatch(
        PolicyActions.loadAllPolicies()
      );
    }
  }

  // ==========================================
  // CREATE POLICY
  // ==========================================

  openCreate(): void {
    this.editingPolicy = null;
    this.selectedStandardPolicyId = '';

    this.loadStandardPolicies();

    this.selectedPolicy = null;

    this.policyForm.reset({
      businessId: 0,
      policyNumber: '',
      policyType: '',
      coverageAmount: 0,
      coverageDetails: '',
      coverageLocation: '',
      premiumAmount: 0,
      startDate: '',
      tenureValue: 1,
      tenureUnit: 'YEARS',
      paymentFrequency: 'MONTHLY',
      description: ''
    });

    this.saving = false;
    this.showForm = true;
  }

  // ==========================================
  // LOAD STANDARD POLICIES
  // ==========================================

  private loadStandardPolicies(): void {
    this.standardPoliciesLoading = true;
    this.standardPoliciesError = '';

    this.policyApi.getStandardPolicies().subscribe({
      next: policies => {
        this.standardPolicies = policies ?? [];
        this.standardPoliciesLoading = false;
      },

      error: error => {
        this.standardPoliciesLoading = false;

        this.standardPoliciesError =
          error?.error?.message ??
          'Unable to load standard policies. You can still create a custom policy.';
      }
    });
  }

  // ==========================================
  // APPLY STANDARD POLICY
  // ==========================================

  applyStandardPolicy(): void {
    const template = this.standardPolicies.find(
      policy => policy.id === this.selectedStandardPolicyId
    );

    if (!template || this.editingPolicy) {
      return;
    }

    const current = this.policyForm.getRawValue();

    const policyNumber =
      current.policyNumber.trim() ||
      `POL-${new Date().getFullYear()}-${Date.now()}`;

    this.policyForm.patchValue({
      policyNumber,
      policyType: template.policyType,
      coverageAmount: template.suggestedCoverageAmount,
      coverageDetails: template.coverageDetails,
      premiumAmount: template.suggestedPremiumAmount,
      description: template.description
    });
  }

  // ==========================================
  // EDIT POLICY
  // ==========================================

  openEdit(policy: Policy): void {
    this.editingPolicy = policy;
    this.selectedPolicy = null;

    this.policyForm.reset({
      businessId: policy.businessId,
      policyNumber: policy.policyNumber,
      policyType: policy.policyType,
      coverageAmount: policy.coverageAmount,
      coverageDetails: policy.coverageDetails ?? '',
      coverageLocation: policy.coverageLocation ?? '',
      premiumAmount: policy.premiumAmount,
      startDate: policy.startDate,
      tenureValue: policy.tenureValue ?? 1,
      tenureUnit: policy.tenureUnit ?? 'YEARS',
      paymentFrequency: policy.paymentFrequency ?? 'YEARLY',
      description: policy.description ?? ''
    });

    this.saving = false;
    this.showForm = true;
  }

  // ==========================================
  // CLOSE POLICY FORM
  // ==========================================

  closeForm(): void {
    if (this.saving) {
      return;
    }

    this.showForm = false;
    this.editingPolicy = null;
  }

  // ==========================================
  // VIEW POLICY
  // ==========================================

  viewPolicy(policy: Policy): void {
    this.selectedPolicy =
      this.selectedPolicy?.id === policy.id
        ? null
        : policy;

    this.cancellationPolicy = null;
    this.cancellationError = '';
    this.cancellationSuccess = '';
  }

  // ==========================================
  // CALCULATE POLICY END DATE
  // ==========================================

  get calculatedEndDate(): string | null {
    const value = this.policyForm.getRawValue();

    if (
      !value.startDate ||
      !value.tenureValue ||
      value.tenureValue < 1 ||
      value.tenureValue > 120 ||
      !value.tenureUnit
    ) {
      return null;
    }

    const startDate = this.parseLocalDate(value.startDate);

    if (!startDate) {
      return null;
    }

    const endDate = new Date(startDate.getTime());

    if (value.tenureUnit === 'MONTHS') {
      endDate.setMonth(
        endDate.getMonth() + value.tenureValue
      );
    } else {
      endDate.setFullYear(
        endDate.getFullYear() + value.tenureValue
      );
    }

    endDate.setDate(endDate.getDate() - 1);

    return this.formatLocalDate(endDate);
  }

  // ==========================================
  // PARSE LOCAL DATE
  // ==========================================

  private parseLocalDate(value: string): Date | null {
    const parts = value.split('-').map(Number);

    if (
      parts.length !== 3 ||
      parts.some(Number.isNaN)
    ) {
      return null;
    }

    const [year, month, day] = parts;

    const date = new Date(
      year,
      month - 1,
      day
    );

    if (
      date.getFullYear() !== year ||
      date.getMonth() !== month - 1 ||
      date.getDate() !== day
    ) {
      return null;
    }

    return date;
  }

  // ==========================================
  // FORMAT LOCAL DATE
  // ==========================================

  private formatLocalDate(date: Date): string {
    const year = date.getFullYear();

    const month = String(
      date.getMonth() + 1
    ).padStart(2, '0');

    const day = String(
      date.getDate()
    ).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  // ==========================================
  // SAVE POLICY
  // ==========================================

  savePolicy(): void {
    if (this.saving) {
      return;
    }

    if (this.policyForm.invalid) {
      this.policyForm.markAllAsTouched();
      return;
    }

    const value = this.policyForm.getRawValue();

    if (!this.calculatedEndDate) {
      return;
    }

    this.saving = true;

    if (this.editingPolicy) {
      const request: UpdatePolicyRequest = {
        businessId: value.businessId,
        policyNumber: value.policyNumber,
        policyType: value.policyType,
        coverageAmount: value.coverageAmount,
        coverageDetails: value.coverageDetails,
        coverageLocation: value.coverageLocation,
        premiumAmount: value.premiumAmount,
        startDate: value.startDate,
        tenureValue: value.tenureValue,
        tenureUnit: value.tenureUnit,
        paymentFrequency: value.paymentFrequency,
        description: value.description
      };

      this.store.dispatch(
        PolicyActions.updatePolicy({
          policyId: this.editingPolicy.id,
          request
        })
      );
    } else {
      const request: CreatePolicyRequest = {
        businessId: value.businessId,
        policyNumber: value.policyNumber,
        policyType: value.policyType,
        coverageAmount: value.coverageAmount,
        coverageDetails: value.coverageDetails,
        coverageLocation: value.coverageLocation,
        premiumAmount: value.premiumAmount,
        startDate: value.startDate,
        tenureValue: value.tenureValue,
        tenureUnit: value.tenureUnit,
        paymentFrequency: value.paymentFrequency,
        description: value.description
      };

      this.store.dispatch(
        PolicyActions.createPolicy({
          request
        })
      );
    }

    this.showForm = false;
    this.editingPolicy = null;
    this.saving = false;
  }

  // ==========================================
  // SUBMIT POLICY FOR REVIEW
  // ==========================================

  submit(policy: Policy): void {
    if (
      !confirm(
        `Submit policy ${policy.policyNumber} for review?`
      )
    ) {
      return;
    }

    this.store.dispatch(
      PolicyActions.submitPolicy({
        policyId: policy.id
      })
    );
  }

  // ==========================================
  // CHANGE POLICY STATUS
  // ==========================================

  changeStatus(
    policy: Policy,
    status: string
  ): void {
    this.store.dispatch(
      PolicyActions.updatePolicyStatus({
        policyId: policy.id,
        status
      })
    );
  }

  // ==========================================
  // OPEN CANCELLATION REQUEST
  // ==========================================

  openCancellationRequest(policy: Policy): void {
    this.cancellationPolicy = policy;
    this.cancellationError = '';
    this.cancellationSuccess = '';

    this.cancellationForm.reset({
      reason: ''
    });
  }

  // ==========================================
  // CLOSE CANCELLATION REQUEST
  // ==========================================

  closeCancellationRequest(): void {
    if (this.cancellationBusy) {
      return;
    }

    this.cancellationPolicy = null;
    this.cancellationError = '';

    this.cancellationForm.reset({
      reason: ''
    });
  }

  // ==========================================
  // SUBMIT CANCELLATION REQUEST
  // ==========================================

  submitCancellationRequest(): void {
    this.requestCancellation();
  }

  // ==========================================
  // REQUEST POLICY CANCELLATION
  // ==========================================

  requestCancellation(): void {
    const policy = this.cancellationPolicy;

    if (!policy || this.cancellationBusy) {
      return;
    }

    if (this.cancellationForm.invalid) {
      this.cancellationForm.markAllAsTouched();
      return;
    }

    const reason =
      this.cancellationForm.controls.reason.value.trim();

    if (!reason) {
      this.cancellationForm.controls.reason.setErrors({
        required: true
      });

      return;
    }

    this.cancellationBusy = true;
    this.cancellationError = '';
    this.cancellationSuccess = '';

    this.policyApi.requestCancellation(
      policy.id,
      { reason }
    ).subscribe({
      next: () => {
        this.cancellationBusy = false;

        this.cancellationSuccess =
          'Cancellation request submitted successfully.';

        this.cancellationPolicy = null;

        this.cancellationForm.reset({
          reason: ''
        });

        this.loadPolicies();
      },

      error: error => {
        this.cancellationBusy = false;

        this.cancellationError =
          error?.error?.message ??
          'Unable to submit cancellation request. Please try again.';
      }
    });
  }

  // ==========================================
  // REVIEW CANCELLATION REQUEST
  // ==========================================

  reviewCancellation(
    policy: Policy,
    approved: boolean
  ): void {
    const action = approved ? 'approve' : 'reject';

    if (
      !confirm(
        `Are you sure you want to ${action} cancellation for policy ${policy.policyNumber}?`
      )
    ) {
      return;
    }

    this.cancellationBusy = true;
    this.cancellationError = '';
    this.cancellationSuccess = '';

    this.policyApi.reviewCancellation(
      policy.id,
      { approved }
    ).subscribe({
      next: () => {
        this.cancellationBusy = false;

        this.cancellationSuccess =
          approved
            ? 'Cancellation approved successfully.'
            : 'Cancellation request rejected.';

        this.loadPolicies();

        if (this.selectedPolicy?.id === policy.id) {
          this.selectedPolicy = {
            ...policy,
            status: approved
              ? 'CANCELLED'
              : policy.status
          };
        }
      },

      error: error => {
        this.cancellationBusy = false;

        this.cancellationError =
          error?.error?.message ??
          `Unable to ${action} cancellation. Please try again.`;
      }
    });
  }

  // ==========================================
  // MAKE POLICY PAYMENT
  // ==========================================

  payForPolicy(policy: Policy): void {
    if (
      this.currentRole !== 'BUSINESS_OWNER' ||
      !['APPROVED', 'ACTIVE'].includes(policy.status) ||
      this.paymentBusyPolicyId !== null
    ) {
      return;
    }

    if (
      !confirm(
        `Make a mock payment of ${policy.premiumAmount} for policy ${policy.policyNumber}?`
      )
    ) {
      return;
    }

    this.paymentBusyPolicyId = policy.id;
    this.paymentError = '';
    this.paymentSuccess = '';
    this.lastPayment = null;

    this.policyApi.makePayment(policy.id).subscribe({
  next: payment => {
    const response = payment as PaymentResponse;

    this.paymentBusyPolicyId = null;
    this.lastPayment = response;

    this.paymentSuccess =
      `Payment successful. Transaction reference: ${response.transactionReference}`;

    this.loadPolicies();
  },

  error: (error: any) => {
    this.paymentBusyPolicyId = null;

    this.paymentError =
      error?.error?.message ??
      'Unable to complete the mock payment. Please try again.';
  }
});
  }

  // ==========================================
  // CANCELLATION PERMISSIONS
  // ==========================================

  canRequestCancellation(policy: Policy): boolean {
    return (
      this.currentRole === 'BUSINESS_OWNER' &&
      ['APPROVED', 'ACTIVE'].includes(policy.status)
    );
  }

  canReviewCancellation(policy: Policy): boolean {
    return (
      ['ADMIN', 'UNDERWRITER'].includes(this.currentRole) &&
      policy.status === 'CANCELLATION_REQUESTED'
    );
  }

  // ==========================================
  // CREATE PERMISSIONS
  // ==========================================

  canCreate(): boolean {
    return [
      'BUSINESS_OWNER',
      'ADMIN'
    ].includes(this.currentRole);
  }

  // ==========================================
  // EDIT PERMISSIONS
  // ==========================================

  canEdit(policy: Policy): boolean {
    return (
      this.currentRole === 'ADMIN' ||
      (
        this.currentRole === 'BUSINESS_OWNER' &&
        policy.status === 'DRAFT'
      )
    );
  }

  // ==========================================
  // SUBMIT PERMISSIONS
  // ==========================================

  canSubmit(policy: Policy): boolean {
    return (
      (
        this.currentRole === 'BUSINESS_OWNER' ||
        this.currentRole === 'ADMIN'
      ) &&
      policy.status === 'DRAFT'
    );
  }

  // ==========================================
  // STATUS CHANGE PERMISSIONS
  // ==========================================

  canChangeStatus(): boolean {
    return [
      'ADMIN',
      'UNDERWRITER'
    ].includes(this.currentRole);
  }

  // ==========================================
  // STATUS CSS CLASS
  // ==========================================

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }
}