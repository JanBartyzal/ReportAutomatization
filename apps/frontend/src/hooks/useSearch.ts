import { useQuery } from '@tanstack/react-query';
import { search, suggest, type SearchParams, type SearchSuggestion, type SearchResult } from '../api/search';

export function useSearch(params: SearchParams) {
    return useQuery({
        queryKey: ['search', params],
        queryFn: () => search(params),
        enabled: !!params.q && params.q.length >= 2,
    });
}

export function useSearchSuggestions(q: string, limit: number = 5) {
    return useQuery<SearchSuggestion[]>({
        queryKey: ['searchSuggestions', q, limit],
        queryFn: () => suggest(q, limit),
        enabled: !!q && q.length >= 2,
    });
}
