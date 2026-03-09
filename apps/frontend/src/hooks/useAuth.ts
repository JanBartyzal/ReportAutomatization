import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getMe, switchOrg } from '../api/auth';
import type { UserContext } from '@reportplatform/types';

export function useMe() {
  return useQuery<UserContext>({
    queryKey: ['auth', 'me'],
    queryFn: getMe,
    staleTime: 5 * 60 * 1000,
  });
}

export function useSwitchOrg() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orgId: string) => switchOrg(orgId),
    onSuccess: () => {
      queryClient.invalidateQueries();
    },
  });
}
