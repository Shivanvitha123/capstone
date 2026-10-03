import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  inject
} from '@angular/core';

import {
  finalize
} from 'rxjs';

import { ApiService } from '../../../core/services/api.service';

interface AuditRecord {
  id: number;
  actorUserId: number | null;
  actorRole: string | null;
  action: string | null;
  entityType: string | null;
  entityId: number | null;
  description: string | null;
  createdAt: string | null;
}

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './audit.component.html',
  styleUrl: './audit.component.css'
})
export class AuditComponent implements OnInit {

  private readonly api =
    inject(ApiService);

  // =========================================================
  // STATE
  // =========================================================

  logs: AuditRecord[] = [];

  loading = false;

  errorMessage = '';

  searchTerm = '';


  // =========================================================
  // INITIALIZATION
  // =========================================================

  ngOnInit(): void {
    this.loadAuditLogs();
  }


  // =========================================================
  // LOAD AUDIT LOGS
  // =========================================================
  //
  // IMPORTANT:
  // The currently configured API Gateway exposes:
  //
  // GET /api/audit
  //
  // for ADMIN.
  //
  // Do not use /api/audit-logs here because that route is
  // not currently configured in the Gateway.
  // =========================================================

  loadAuditLogs(): void {

    this.loading = true;

    this.errorMessage = '';


    this.api
      .get<AuditRecord[]>(
        '/api/audit'
      )
      .pipe(

        // ---------------------------------------------------
        // ALWAYS stop the spinner.
        //
        // This executes for both:
        //  - successful response
        //  - failed response
        // ---------------------------------------------------

        finalize(() => {
          this.loading = false;
        })

      )
      .subscribe({

        next: (logs) => {

          this.logs =
            Array.isArray(logs)
              ? logs
              : [];

          this.errorMessage = '';

          console.log(
            'Audit logs loaded:',
            this.logs
          );

        },

        error: (error: unknown) => {

          console.error(
            'Failed to load audit logs:',
            error
          );


          this.logs = [];


          this.errorMessage =
            'Unable to load audit logs. Please check your access and try again.';

        }

      });

  }


  // =========================================================
  // SEARCH
  // =========================================================

  onSearch(event: Event): void {

    const input =
      event.target as HTMLInputElement;

    this.searchTerm =
      input.value;

  }


  // =========================================================
  // FILTERED LOGS
  // =========================================================

  get filteredLogs(): AuditRecord[] {

    const query =
      this.searchTerm
        .trim()
        .toLowerCase();


    if (!query) {

      return this.logs;

    }


    return this.logs.filter(
      (log) => {

        return [

          log.id,

          log.actorUserId,

          log.actorRole,

          log.action,

          log.entityType,

          log.entityId,

          log.description,

          log.createdAt

        ]

          .join(' ')

          .toLowerCase()

          .includes(query);

      }
    );

  }


  // =========================================================
  // UNIQUE ACTORS
  // =========================================================

  get uniqueActors(): number {

    return new Set(

      this.logs

        .map(
          log => log.actorUserId
        )

        .filter(
          (id): id is number =>
            id !== null
        )

    ).size;

  }


  // =========================================================
  // ACTION COUNT
  // =========================================================

  get actionCount(): number {

    return this.logs.length;

  }


  // =========================================================
  // TRACK BY
  // =========================================================

  trackByLogId(
    index: number,
    log: AuditRecord
  ): number {

    return log.id;

  }

}