import apiClient from './axios';
import type { PaginationParams } from '@reportplatform/types';

export type SearchMode = 'text' | 'semantic';
export type EntityType = 'FILE' | 'DOCUMENT' | 'REPORT' | 'FORM';

export interface SearchParams extends PaginationParams {
  q: string;
  type?: EntityType;
  mode?: SearchMode;
}

export interface SearchResult {
  id: string;
  type: string;
  title: string;
  snippet: string;
  score: number;
  highlight: string;
  metadata: Record<string, unknown>;
}

export interface SearchSuggestion {
  text: string;
  type: string;
  id: string;
}

export async function search(params: SearchParams): Promise<{
  data: SearchResult[];
  pagination: {
    page: number;
    page_size: number;
    total_items: number;
    total_pages: number
  }
}> {
  const { data } = await apiClient.get('/search', { params });
  return data;
}

export async function suggest(q: string, limit: number = 5): Promise<SearchSuggestion[]> {
  const { data } = await apiClient.get<SearchSuggestion[]>('/search/suggest', { params: { q, limit } });
  return data;
}
