import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useMsal } from '@azure/msal-react';
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

export function useLogout() {
  const { instance } = useMsal();

  return {
    mutate: () => {
      instance.logoutPopup().catch((error) => {
        console.error('Logout failed:', error);
      });
    },
    mutateAsync: async () => {
      await instance.logoutPopup();
    },
    isPending: false,
    isError: false,
    isSuccess: false,
  };
}

// Combined auth hook for convenience
export function useAuth() {
  const me = useMe();
  const logout = useLogout();
  return {
    ...me,
    logout,
  };
}
