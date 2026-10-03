import { UserRole } from '../constants/roles';

/**
 * Authentication request DTO.
 * Represents the data sent from the frontend to the authentication API.
 */
export interface LoginRequestDto {
  email: string;
  password: string;
}

/**
 * Registration request DTO.
 * Represents the data sent when a new user registers.
 */
export interface RegisterRequestDto {
  name: string;
  email: string;
  password: string;
}

/**
 * Authentication response DTO.
 * Represents the authenticated user information returned by the backend.
 */
export interface AuthResponseDto {
  userId: number;
  name: string;
  email: string;
  role: UserRole;
}