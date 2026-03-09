/** Standard API response wrapper */
export interface ApiResponse<T> {
  data: T;
  timestamp: string; // ISO 8601
}

/** Paginated API response */
export interface PaginatedResponse<T> {
  data: T[];
  pagination: PaginationMeta;
}

export interface PaginationMeta {
  page: number;
  page_size: number;
  total_items: number;
  total_pages: number;
}

/** Standard pagination query parameters */
export interface PaginationParams {
  page?: number;
  page_size?: number;
}

/** Standard API error */
export interface ApiError {
  error: string;
  message: string;
  details?: Record<string, unknown>;
}

/** Processing status used across services */
export enum ProcessingStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  PARTIAL = 'PARTIAL',
}
