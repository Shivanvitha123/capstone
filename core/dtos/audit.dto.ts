/**
 * Audit record response DTO.
 *
 * Represents an activity event returned by the
 * Notification/Audit service.
 */
export interface AuditRecordDto {
  id: number;
  actorUserId: number | null;
  actorRole: string | null;
  action: string | null;
  entityType: string | null;
  entityId: number | null;
  description: string | null;
  createdAt: string | null;
}