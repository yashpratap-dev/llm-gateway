export const PAGES = {
  OVERVIEW:  'overview',
  PROVIDERS: 'providers',
  TENANTS:   'tenants',
  ANALYTICS: 'analytics',
} as const;

export type PageKey = typeof PAGES[keyof typeof PAGES];
