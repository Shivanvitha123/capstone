/**
 * Notification type DTO.
 */
export type NotificationTypeDto =
  | 'INFO'
  | 'SUCCESS'
  | 'WARNING'
  | 'ACTION_REQUIRED';

/**
 * Notification response DTO.
 */
export interface NotificationDto {
  id: number;
  recipientUserId: number;
  type: NotificationTypeDto;
  title: string;
  message: string;
  referenceType: string | null;
  referenceId: number | null;
  read: boolean;
  createdAt: string;
}