import { UserRole } from '../constants/roles';

/**
 * User DTO.
 * Represents user information transferred between
 * the frontend and backend.
 */
export interface UserDto {
  userId: number;
  name: string;
  email: string;
  role: UserRole;
}