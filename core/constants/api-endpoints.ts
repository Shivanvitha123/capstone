export const API = {
  AUTH: {
    LOGIN: '/api/auth/login',
    REGISTER: '/api/auth/register',
    LOGOUT: '/api/auth/logout',
    ME: '/api/auth/me'
  },

  BUSINESS: '/api/business',

  POLICIES: '/api/policies',

  RISK: '/api/risk',

  CLAIMS: '/api/claims',

  USERS: '/api/auth/users',

  AUDIT: '/api/audit',

  ADMIN_ANALYTICS: '/api/admin/analytics/dashboard',

  NOTIFICATIONS: '/api/notifications',

  EVENTS: '/api/events'
} as const;