import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { Store } from '@ngrx/store';

import { finalize } from 'rxjs';

import {
  Claim,
  ClaimStatus,
  ClaimType,
  Recovery
} from '../../models/claims.models';

import { ClaimsApiService } from '../../services/claims-api.service';

import * as ClaimsActions from '../../store/claims.actions';

import {
  selectClaims,
  selectClaimsLoading,
  selectClaimsCreating,
  selectClaimsUpdating,
  selectClaimsError,
  selectClaimsSuccessMessage,
  selectSelectedClaimRecoveries
} from '../../store/claims.selectors';

@Component({
  selector: 'app-claims',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './claims.component.html',
  styleUrl: './claims.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ClaimsComponent implements OnInit {

  private readonly store = inject(Store);
  private readonly fb = inject(FormBuilder);
  private readonly claimsApi = inject(ClaimsApiService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly claims$ =
    this.store.select(selectClaims);

  readonly loading$ =
    this.store.select(selectClaimsLoading);

  readonly creating$ =
    this.store.select(selectClaimsCreating);

  readonly updating$ =
    this.store.select(selectClaimsUpdating);

  readonly error$ =
    this.store.select(selectClaimsError);

  readonly successMessage$ =
    this.store.select(selectClaimsSuccessMessage);

  readonly recoveries$ =
    this.store.select(selectSelectedClaimRecoveries);

  activeTab: 'claims' | 'recoveries' = 'claims';

  selectedClaim: Claim | null = null;

  showCreateClaim = false;
  showStatusModal = false;
  showRecoveryModal = false;

  selectedRecovery: Recovery | null = null;

  // ---------------- AUTHENTICATED USER ----------------

  currentRole: string | null = null;
  loadingUser = true;

  // ---------------- CLAIM DOCUMENTS ----------------

  selectedFile: File | null = null;

  uploading = false;
  loadingDocuments = false;

  uploadMessage = '';
  uploadError = '';

  uploadedDocuments: string[] = [];

  // -------------------------------------------------

  readonly claimTypes: ClaimType[] = [
    'PROPERTY_DAMAGE',
    'FIRE',
    'FLOOD',
    'THEFT',
    'LIABILITY',
    'ACCIDENT',
    'OTHER'
  ];

  readonly claimStatuses: ClaimStatus[] = [
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED',
    'SETTLED'
  ];

  // ---------------- CLAIM FORM ----------------

  readonly claimForm = this.fb.nonNullable.group({
    businessId: [
      0,
      [
        Validators.required,
        Validators.min(1)
      ]
    ],
    policyId: [
      0,
      [
        Validators.required,
        Validators.min(1)
      ]
    ],
    claimNumber: [
      '',
      Validators.required
    ],
    claimType: [
      'PROPERTY_DAMAGE' as ClaimType,
      Validators.required
    ],
    incidentDate: [
      '',
      Validators.required
    ],
    reportedDate: [
      '',
      Validators.required
    ],
    claimedAmount: [
      0,
      [
        Validators.required,
        Validators.min(0.01)
      ]
    ],
    description: ['']
  });

  // ---------------- STATUS FORM ----------------

  readonly statusForm = this.fb.nonNullable.group({
    status: [
      'UNDER_REVIEW' as ClaimStatus,
      Validators.required
    ],
    approvedAmount: [0],
    assignedAdjusterId: [0]
  });

  // ---------------- RECOVERY FORM ----------------

  readonly recoveryForm = this.fb.nonNullable.group({
    recoveryAmount: [
      0,
      [
        Validators.required,
        Validators.min(0.01)
      ]
    ],
    recoverySource: [''],
    description: ['']
  });

  readonly recoveryStatusForm = this.fb.nonNullable.group({
    status: [
      '',
      Validators.required
    ]
  });

  // ---------------- INITIALIZATION ----------------

  ngOnInit(): void {
    this.loadCurrentUser();
  }

  /**
   * Retrieves the authenticated user's role from the
   * backend session endpoint.
   *
   * The JWT is stored in an HttpOnly cookie and is
   * intentionally not read from localStorage.
   */
  private loadCurrentUser(): void {
    this.loadingUser = true;
    this.cdr.markForCheck();

    this.claimsApi.getCurrentUser()
      .pipe(
        finalize(() => {
          this.loadingUser = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (user) => {
          const role =
            user?.role ??
            user?.authorities?.[0] ??
            '';

          this.currentRole = role
            .replace(/^ROLE_/, '')
            .toUpperCase();

          this.loadClaims();
          this.cdr.markForCheck();
        },

        error: (error) => {
          console.error(
            'Unable to retrieve authenticated user:',
            error
          );

          this.currentRole = null;
          this.cdr.markForCheck();
        }
      });
  }

  // ---------------- ROLE ----------------

  get role(): string | null {
    return this.currentRole;
  }

  get canCreateClaim(): boolean {
    return (
      this.role === 'BUSINESS_OWNER' ||
      this.role === 'ADMIN'
    );
  }

  get canManageClaims(): boolean {
    return (
      this.role === 'CLAIMS_ADJUSTER' ||
      this.role === 'ADMIN'
    );
  }

  get canManageRecovery(): boolean {
    return (
      this.role === 'CLAIMS_ADJUSTER' ||
      this.role === 'ADMIN'
    );
  }

  // ---------------- LOAD CLAIMS ----------------

  /**
   * Business Owners load only their own claims.
   * Claims Adjusters and Admins load all claims.
   * Unknown roles do not trigger an unrestricted request.
   */
  loadClaims(): void {
    if (this.role === 'BUSINESS_OWNER') {
      this.store.dispatch(
        ClaimsActions.loadOwnerClaims()
      );
      return;
    }

    if (
      this.role === 'CLAIMS_ADJUSTER' ||
      this.role === 'ADMIN'
    ) {
      this.store.dispatch(
        ClaimsActions.loadClaims()
      );
      return;
    }

    console.error(
      'Claims cannot be loaded. Unsupported or missing role:',
      this.role
    );
  }

  // ---------------- CREATE CLAIM ----------------

  openCreateClaim(): void {
    if (!this.canCreateClaim) {
      return;
    }

    this.claimForm.reset({
      businessId: 0,
      policyId: 0,
      claimNumber: '',
      claimType: 'PROPERTY_DAMAGE',
      incidentDate: '',
      reportedDate: '',
      claimedAmount: 0,
      description: ''
    });

    this.showCreateClaim = true;
  }

  closeCreateClaim(): void {
    this.showCreateClaim = false;
  }

  submitClaim(): void {
    if (!this.canCreateClaim) {
      return;
    }

    if (this.claimForm.invalid) {
      this.claimForm.markAllAsTouched();
      return;
    }

    this.store.dispatch(
      ClaimsActions.createClaim({
        request: this.claimForm.getRawValue()
      })
    );

    this.showCreateClaim = false;
  }

  // ---------------- CLAIM DETAILS ----------------

  viewClaim(claim: Claim): void {
    this.selectedClaim = claim;

    this.activeTab = 'claims';

    this.selectedFile = null;
    this.uploadMessage = '';
    this.uploadError = '';
    this.uploadedDocuments = [];

    this.store.dispatch(
      ClaimsActions.loadClaimRecoveries({
        claimId: claim.id
      })
    );

    this.loadClaimDocuments(claim.id);

    this.cdr.markForCheck();
  }

  closeClaimDetails(): void {
    this.selectedClaim = null;

    this.selectedFile = null;
    this.uploadMessage = '';
    this.uploadError = '';
    this.uploadedDocuments = [];
    this.loadingDocuments = false;

    this.cdr.markForCheck();
  }

  // ---------------- UPDATE CLAIM STATUS ----------------

  openStatusModal(claim: Claim): void {
    if (!this.canManageClaims) {
      return;
    }

    this.selectedClaim = claim;

    this.statusForm.reset({
      status: claim.status,
      approvedAmount:
        claim.approvedAmount ?? 0,
      assignedAdjusterId:
        claim.assignedAdjusterId ?? 0
    });

    this.showStatusModal = true;
  }

  closeStatusModal(): void {
    this.showStatusModal = false;
  }

  updateClaimStatus(): void {
    if (!this.canManageClaims || !this.selectedClaim) {
      return;
    }

    if (this.statusForm.invalid) {
      this.statusForm.markAllAsTouched();
      return;
    }

    const value = this.statusForm.getRawValue();

    const request = {
      status: value.status,
      approvedAmount:
        value.approvedAmount > 0
          ? value.approvedAmount
          : undefined,
      assignedAdjusterId:
        value.assignedAdjusterId > 0
          ? value.assignedAdjusterId
          : undefined
    };

    this.store.dispatch(
      ClaimsActions.updateClaimStatus({
        claimId: this.selectedClaim.id,
        request
      })
    );

    this.showStatusModal = false;
  }

  // ---------------- RECOVERY ----------------

  openRecoveryModal(claim: Claim): void {
    if (!this.canManageRecovery) {
      return;
    }

    this.selectedClaim = claim;

    this.recoveryForm.reset({
      recoveryAmount: 0,
      recoverySource: '',
      description: ''
    });

    this.showRecoveryModal = true;
  }

  closeRecoveryModal(): void {
    this.showRecoveryModal = false;
  }

  createRecovery(): void {
    if (
      !this.canManageRecovery ||
      !this.selectedClaim
    ) {
      return;
    }

    if (this.recoveryForm.invalid) {
      this.recoveryForm.markAllAsTouched();
      return;
    }

    this.store.dispatch(
      ClaimsActions.createRecovery({
        claimId: this.selectedClaim.id,
        request: this.recoveryForm.getRawValue()
      })
    );

    this.showRecoveryModal = false;
  }

  selectRecoveries(): void {
    if (!this.selectedClaim) {
      return;
    }

    this.activeTab = 'recoveries';

    this.store.dispatch(
      ClaimsActions.loadClaimRecoveries({
        claimId: this.selectedClaim.id
      })
    );
  }

  openRecoveryStatus(recovery: Recovery): void {
    if (!this.canManageRecovery) {
      return;
    }

    this.selectedRecovery = recovery;

    this.recoveryStatusForm.reset({
      status: recovery.status
    });
  }

  updateRecoveryStatus(): void {
    if (
      !this.canManageRecovery ||
      !this.selectedRecovery
    ) {
      return;
    }

    if (this.recoveryStatusForm.invalid) {
      this.recoveryStatusForm.markAllAsTouched();
      return;
    }

    this.store.dispatch(
      ClaimsActions.updateRecoveryStatus({
        recoveryId: this.selectedRecovery.id,
        request: this.recoveryStatusForm.getRawValue()
      })
    );

    this.selectedRecovery = null;
  }

  // ---------------- CLAIM DOCUMENTS ----------------

  /**
   * Fetches document filenames saved for a claim.
   * Called whenever a claim is opened.
   */
  loadClaimDocuments(claimId: number): void {
    this.loadingDocuments = true;
    this.uploadError = '';

    this.cdr.markForCheck();

    this.claimsApi
      .getClaimDocuments(claimId)
      .pipe(
        finalize(() => {
          this.loadingDocuments = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (documents) => {
          if (this.selectedClaim?.id !== claimId) {
            return;
          }

          this.uploadedDocuments =
            Array.isArray(documents)
              ? documents
              : [];

          this.cdr.markForCheck();
        },

        error: (error) => {
          console.error(
            'Unable to load claim documents:',
            error
          );

          if (this.selectedClaim?.id !== claimId) {
            return;
          }

          this.uploadedDocuments = [];

          this.uploadError =
            error?.error?.message ||
            'Unable to load documents for this claim.';

          this.cdr.markForCheck();
        }
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    this.selectedFile = null;
    this.uploadMessage = '';
    this.uploadError = '';

    if (!file) {
      this.cdr.markForCheck();
      return;
    }

    const allowedExtensions = [
      'pdf',
      'png',
      'jpg',
      'jpeg',
      'docx'
    ];

    const extension = file.name
      .split('.')
      .pop()
      ?.toLowerCase();

    if (
      !extension ||
      !allowedExtensions.includes(extension)
    ) {
      this.uploadError =
        'Only PDF, PNG, JPG, JPEG, and DOCX files are allowed.';

      input.value = '';
      this.cdr.markForCheck();
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.uploadError =
        'File size must not exceed 10 MB.';

      input.value = '';
      this.cdr.markForCheck();
      return;
    }

    this.selectedFile = file;
    this.cdr.markForCheck();
  }

  uploadDocument(): void {
    if (
      !this.selectedClaim ||
      !this.selectedFile ||
      this.uploading
    ) {
      return;
    }

    const claimId = this.selectedClaim.id;
    const file = this.selectedFile;

    this.uploading = true;
    this.uploadMessage = '';
    this.uploadError = '';

    this.cdr.markForCheck();

    this.claimsApi
      .uploadClaimDocument(claimId, file)
      .pipe(
        finalize(() => {
          this.uploading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (filename) => {
          if (this.selectedClaim?.id !== claimId) {
            return;
          }

          const savedFilename =
            typeof filename === 'string'
              ? filename.trim()
              : '';

          if (!savedFilename) {
            this.uploadError =
              'The server accepted the upload but did not return a filename.';

            this.cdr.markForCheck();
            return;
          }

          this.selectedFile = null;
          this.uploadMessage =
            'Document uploaded successfully.';

          this.loadClaimDocuments(claimId);

          this.cdr.markForCheck();
        },

        error: (error) => {
          console.error(
            'Document upload failed:',
            error
          );

          this.uploadError =
            error?.error?.message ||
            (typeof error?.error === 'string'
              ? error.error
              : null) ||
            'Document upload failed. Please try again.';

          this.cdr.markForCheck();
        }
      });
  }

  downloadDocument(filename: string): void {
    if (!this.selectedClaim) {
      return;
    }

    const claimId = this.selectedClaim.id;

    this.uploadError = '';

    this.claimsApi
      .downloadClaimDocument(claimId, filename)
      .subscribe({
        next: (blob) => {
          const url =
            window.URL.createObjectURL(blob);

          const anchor =
            document.createElement('a');

          anchor.href = url;
          anchor.download = filename;

          document.body.appendChild(anchor);
          anchor.click();
          anchor.remove();

          window.URL.revokeObjectURL(url);
        },

        error: (error) => {
          console.error(
            'Document download failed:',
            error
          );

          this.uploadError =
            error?.error?.message ||
            'Unable to download the document.';

          this.cdr.markForCheck();
        }
      });
  }

  // ---------------- DISPLAY HELPERS ----------------

  getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED':
      case 'SETTLED':
        return 'status-success';

      case 'REJECTED':
        return 'status-danger';

      case 'UNDER_REVIEW':
        return 'status-warning';

      default:
        return 'status-neutral';
    }
  }

  formatCurrency(value: number | null): string {
    return new Intl.NumberFormat(
      'en-IN',
      {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2
      }
    ).format(value ?? 0);
  }

  countStatus(
    claims: Claim[],
    status: ClaimStatus
  ): number {
    return claims.filter(
      claim => claim.status === status
    ).length;
  }
}