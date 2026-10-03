import { Component, inject } from '@angular/core';

import {
  AsyncPipe,
  CurrencyPipe,
  DatePipe
} from '@angular/common';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { HttpErrorResponse } from '@angular/common/http';

import { Store } from '@ngrx/store';

import {
  filter,
  finalize,
  map,
  Observable,
  take
} from 'rxjs';

import * as BusinessActions
  from '../../store/business.actions';

import {
  selectBusinesses,
  selectSummaries,
  selectBusinessError,
  selectLoading,
  selectSaving
} from '../../store/business.selector';

import { PageHeaderComponent }
  from '../../../../shared/components/page-header.component/page-header.component';

import {
  BusinessApiService,
  BusinessDeletionRequest
} from '../../services/business-api.service';

import { selectAuth }
  from '../../../auth/store/auth.selector';

import { loadCurrentUser }
  from '../../../auth/store/auth.actions';


@Component({
  selector: 'app-business',
  standalone: true,

  imports: [
    AsyncPipe,
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    PageHeaderComponent
  ],

  templateUrl: './business.component.html',

  styleUrl: './business.component.css'
})
export class BusinessComponent {

  private readonly store =
    inject(Store);

  private readonly fb =
    inject(FormBuilder);

  private readonly businessApi =
    inject(BusinessApiService);


  // =========================================================
  // STORE SELECTORS
  // =========================================================

  readonly businesses$ =
    this.store.select(selectBusinesses);

  readonly summaries$ =
    this.store.select(selectSummaries);

  readonly loading$ =
    this.store.select(selectLoading);

  readonly saving$ =
    this.store.select(selectSaving);

  readonly error$ =
    this.store.select(selectBusinessError);


  // =========================================================
  // AUTH / ROLE
  // =========================================================

  role = '';

  canViewAllBusinesses = false;

  isReviewer = false;

  canManageBusiness = false;


  // =========================================================
  // BUSINESS UI STATE
  // =========================================================

  editingId: number | null = null;

  showForm = false;


  // =========================================================
  // DELETION REQUEST STATE
  // =========================================================

  showDeletionForm = false;

  selectedBusinessId: number | null = null;

  deletionRequests: BusinessDeletionRequest[] = [];

  deletionLoading = false;

  deletionSubmitting = false;

  deletionError = '';

  deletionSuccess = '';

  canReviewDeletionRequests = false;

  reviewingRequestIds =
    new Set<number>();


  // =========================================================
  // BUSINESS FORM
  // =========================================================

  readonly form =
    this.fb.nonNullable.group({

      // Basic information

      businessName: [
        '',
        Validators.required
      ],

      registrationNumber: [
        '',
        Validators.required
      ],

      businessType: [
        '',
        Validators.required
      ],

      industry: [
        '',
        Validators.required
      ],


      // Address

      address: [
        '',
        Validators.required
      ],

      city: [
        '',
        Validators.required
      ],

      state: [
        '',
        Validators.required
      ],

      postalCode: [
        '',
        Validators.required
      ],

      country: [
        'India',
        Validators.required
      ],


      // Contact

      contactEmail: [
        '',
        [
          Validators.required,
          Validators.email
        ]
      ],

      contactPhone: [
        '',
        Validators.required
      ],


      // Metrics

      annualRevenue: [
        0,
        [
          Validators.required,
          Validators.min(0)
        ]
      ],

      employeeCount: [
        1,
        [
          Validators.required,
          Validators.min(1)
        ]
      ],

      establishedDate: [
        '',
        Validators.required
      ],


      // Additional business information

      businessDescription: [
        '',
        Validators.maxLength(2000)
      ],

      website: [
        '',
        Validators.maxLength(255)
      ],

      branchCount: [
        0,
        Validators.min(0)
      ],

      premisesType: [
        ''
      ],

      previousInsuranceClaims: [
        0,
        Validators.min(0)
      ],

      existingInsurance: [
        false
      ]

    });


  // =========================================================
  // DELETION FORM
  // =========================================================

  readonly deletionForm =
    this.fb.nonNullable.group({

      reason: [
        '',
        [
          Validators.required,
          Validators.maxLength(1000)
        ]
      ]

    });


  // =========================================================
  // CONSTRUCTOR
  // =========================================================

  constructor() {

    this.initializeUser();

  }


  // =========================================================
  // AUTH INITIALIZATION
  // =========================================================

  private initializeUser(): void {

    /*
     * Ask the auth store to restore the current user if
     * authentication has not been initialized yet.
     */

    this.store
      .select(selectAuth)
      .pipe(
        take(1)
      )
      .subscribe((state) => {

        if (!state.initialized) {

          this.store.dispatch(
            loadCurrentUser()
          );

        }

      });


    /*
     * IMPORTANT:
     *
     * Do NOT load the business before the auth state has
     * been initialized.
     *
     * The backend uses the authenticated user to determine
     * which businesses belong to the owner.
     */

    this.store
      .select(selectAuth)
      .pipe(
        filter(
          (state) =>
            state.initialized
        ),
        take(1)
      )
      .subscribe((state) => {

        const rawRole =
          state.user?.role ?? '';


        this.role =
          String(rawRole)
            .replace(/^ROLE_/i, '')
            .toUpperCase();


        this.canViewAllBusinesses =
          this.role === 'ADMIN' ||
          this.role === 'UNDERWRITER' ||
          this.role === 'RISK_ENGINEER';


        this.isReviewer =
          this.role === 'ADMIN' ||
          this.role === 'UNDERWRITER';


        this.canManageBusiness =
          this.role === 'BUSINESS_OWNER';


        /*
         * NOW load the business data.
         */
        this.load();


        /*
         * Load deletion requests only for users
         * who are allowed to access them.
         */

        if (
          this.canManageBusiness ||
          this.isReviewer
        ) {

          this.loadDeletionRequests();

        }

      });

  }


  // =========================================================
  // LOAD BUSINESSES
  // =========================================================

  load(): void {

    /*
     * ADMIN / UNDERWRITER / RISK_ENGINEER
     *
     * These roles can see all businesses.
     */

    if (this.canViewAllBusinesses) {

      this.store.dispatch(
        BusinessActions.loadAllBusinesses()
      );

      return;

    }


    /*
     * BUSINESS OWNER
     *
     * This calls:
     *
     * GET /api/business/me
     *
     * Therefore refreshing the browser restores the
     * persisted business from the backend.
     */

    if (this.canManageBusiness) {

      this.store.dispatch(
        BusinessActions.loadBusinesses()
      );

      return;

    }


    /*
     * Unknown / unsupported role.
     */

    console.warn(
      'Business page loaded without a supported role:',
      this.role
    );

  }


  // =========================================================
  // GET BUSINESS FROM STORE
  // =========================================================

  getBusiness(id: number) {

    let result: any = undefined;

    this.store
      .select(selectBusinesses)
      .pipe(
        take(1)
      )
      .subscribe((items) => {

        result =
          items.find(
            (item) =>
              item.id === id
          );

      });

    return result;

  }


  // =========================================================
  // CREATE FORM
  // =========================================================

  openCreate(): void {

    if (!this.canManageBusiness) {
      return;
    }


    this.editingId = null;


    this.form.reset({

      businessName: '',

      registrationNumber: '',

      businessType: '',

      industry: '',

      address: '',

      city: '',

      state: '',

      postalCode: '',

      country: 'India',

      contactEmail: '',

      contactPhone: '',

      annualRevenue: 0,

      employeeCount: 1,

      establishedDate: '',

      businessDescription: '',

      website: '',

      branchCount: 0,

      premisesType: '',

      previousInsuranceClaims: 0,

      existingInsurance: false

    });


    this.showForm = true;

  }


  // =========================================================
  // EDIT BUSINESS
  // =========================================================

  edit(id: number): void {

    if (!this.canManageBusiness) {
      return;
    }


    const business =
      this.getBusiness(id);


    if (!business) {

      console.warn(
        'Business not found in store:',
        id
      );

      return;

    }


    this.editingId = id;


    this.form.patchValue({

      businessName:
        business.businessName,

      registrationNumber:
        business.registrationNumber,

      businessType:
        business.businessType,

      industry:
        business.industry,

      address:
        business.address,

      city:
        business.city,

      state:
        business.state,

      postalCode:
        business.postalCode,

      country:
        business.country,

      contactEmail:
        business.contactEmail,

      contactPhone:
        business.contactPhone,

      annualRevenue:
        business.annualRevenue,

      employeeCount:
        business.employeeCount,

      establishedDate:
        business.establishedDate,

      businessDescription:
        business.businessDescription ?? '',

      website:
        business.website ?? '',

      branchCount:
        business.branchCount ?? 0,

      premisesType:
        business.premisesType ?? '',

      previousInsuranceClaims:
        business.previousInsuranceClaims ?? 0,

      existingInsurance:
        business.existingInsurance ?? false

    });


    this.showForm = true;

  }


  // =========================================================
  // CLOSE FORM
  // =========================================================

  closeForm(): void {

    this.showForm = false;

    this.editingId = null;

  }


  // =========================================================
  // SAVE BUSINESS
  // =========================================================

  save(): void {

    if (!this.canManageBusiness) {

      return;

    }


    if (this.form.invalid) {

      this.form.markAllAsTouched();

      return;

    }


    const request =
      this.form.getRawValue();


    /*
     * UPDATE
     */

    if (this.editingId !== null) {

      this.store.dispatch(

        BusinessActions.updateBusiness({

          id:
            this.editingId,

          request

        })

      );

    }

    /*
     * CREATE
     */

    else {

      this.store.dispatch(

        BusinessActions.createBusiness({

          request

        })

      );

    }


    this.showForm = false;

  }


  // =========================================================
  // DELETION REQUESTS
  // =========================================================

  loadDeletionRequests(): void {

    if (
      !this.canManageBusiness &&
      !this.isReviewer
    ) {

      this.deletionRequests = [];

      this.canReviewDeletionRequests =
        false;

      return;

    }


    this.deletionLoading = true;

    this.deletionError = '';


    this.getDeletionRequests()
      .pipe(

        finalize(() => {

          this.deletionLoading =
            false;

        })

      )
      .subscribe({

        next: (result) => {

          this.deletionRequests =
            result.requests;

          this.canReviewDeletionRequests =
            result.reviewer;

        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.deletionRequests = [];

          this.canReviewDeletionRequests =
            false;

          this.deletionError =
            error?.error?.message ||
            'Could not load deletion requests. Please try again.';

        }

      });

  }


  // =========================================================
  // GET DELETION REQUESTS
  // =========================================================

  private getDeletionRequests():
    Observable<{
      requests:
        BusinessDeletionRequest[];

      reviewer:
        boolean;
    }> {

    if (this.isReviewer) {

      return this.businessApi
        .getPendingDeletionRequests()
        .pipe(

          map((requests) => ({

            requests,

            reviewer: true

          }))

        );

    }


    return this.businessApi
      .getMyDeletionRequests()
      .pipe(

        map((requests) => ({

          requests,

          reviewer: false

        }))

      );

  }


  // =========================================================
  // CHECK PENDING DELETION
  // =========================================================

  hasPendingRequest(
    businessId: number
  ): boolean {

    return this.deletionRequests.some(

      (request) =>

        request.businessId ===
          businessId &&

        request.status
          ?.toUpperCase() ===
          'PENDING'

    );

  }


  // =========================================================
  // OPEN DELETION FORM
  // =========================================================

  openDeletionForm(
    businessId: number
  ): void {

    if (!this.canManageBusiness) {
      return;
    }


    this.selectedBusinessId =
      businessId;


    this.deletionForm.reset({
      reason: ''
    });


    this.deletionError = '';

    this.deletionSuccess = '';


    this.showDeletionForm =
      true;

  }


  // =========================================================
  // CLOSE DELETION FORM
  // =========================================================

  closeDeletionForm(): void {

    this.showDeletionForm =
      false;

    this.selectedBusinessId =
      null;

    this.deletionForm.reset({
      reason: ''
    });

  }


  // =========================================================
  // SUBMIT DELETION REQUEST
  // =========================================================

  submitDeletionRequest(): void {

    if (

      !this.canManageBusiness ||

      this.deletionForm.invalid ||

      this.selectedBusinessId === null ||

      this.deletionSubmitting

    ) {

      this.deletionForm
        .markAllAsTouched();

      return;

    }


    const businessId =
      this.selectedBusinessId;


    const reason =
      this.deletionForm
        .getRawValue()
        .reason
        .trim();


    if (!reason) {

      this.deletionForm.controls.reason
        .setErrors({
          required: true
        });

      return;

    }


    this.deletionSubmitting =
      true;

    this.deletionError = '';

    this.deletionSuccess = '';


    this.businessApi
      .requestDeletion(
        businessId,
        reason
      )
      .pipe(

        finalize(() => {

          this.deletionSubmitting =
            false;

        })

      )
      .subscribe({

        next: () => {

          this.deletionSuccess =
            'Deletion request submitted successfully.';

          this.closeDeletionForm();

          this.loadDeletionRequests();

        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.deletionError =
            error?.error?.message ||
            'Failed to submit deletion request.';

        }

      });

  }


  // =========================================================
  // REVIEW STATE
  // =========================================================

  isReviewing(
    requestId: number
  ): boolean {

    return this.reviewingRequestIds
      .has(requestId);

  }


  // =========================================================
  // APPROVE
  // =========================================================

  approveDeletionRequest(
    request: BusinessDeletionRequest
  ): void {

    if (

      !this.canReviewDeletionRequests ||

      this.isReviewing(request.id) ||

      request.status
        ?.toUpperCase() !==
        'PENDING'

    ) {

      return;

    }


    this.reviewRequest(
      request.id,
      'approve'
    );

  }


  // =========================================================
  // REJECT
  // =========================================================

  rejectDeletionRequest(
    request: BusinessDeletionRequest
  ): void {

    if (

      !this.canReviewDeletionRequests ||

      this.isReviewing(request.id) ||

      request.status
        ?.toUpperCase() !==
        'PENDING'

    ) {

      return;

    }


    this.reviewRequest(
      request.id,
      'reject'
    );

  }


  // =========================================================
  // REVIEW REQUEST
  // =========================================================

  private reviewRequest(
    requestId: number,
    action: 'approve' | 'reject'
  ): void {

    this.deletionError = '';

    this.deletionSuccess = '';


    this.reviewingRequestIds.add(
      requestId
    );


    const request$ =
      action === 'approve'

        ? this.businessApi
            .approveDeletionRequest(
              requestId
            )

        : this.businessApi
            .rejectDeletionRequest(
              requestId
            );


    request$
      .pipe(

        finalize(() => {

          this.reviewingRequestIds
            .delete(requestId);

        })

      )
      .subscribe({

        next: () => {

          this.deletionSuccess =
            action === 'approve'

              ? 'Deletion request approved successfully.'

              : 'Deletion request rejected successfully.';


          this.loadDeletionRequests();

          this.load();

        },

        error: (
          error: HttpErrorResponse
        ) => {

          this.deletionError =
            error?.error?.message ||
            `Failed to ${action} deletion request.`;

        }

      });

  }

}