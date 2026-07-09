// Every route/component fetches API data through these hooks instead of its
// own useEffect+useState. Loading/error state, caching, de-duplication, and
// refetch-on-window-focus all come from React Query for free; the query keys
// below are the single source of truth for how that cache is organized.

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  type Composition,
  type CompositionInput,
  type UserProfile,
  createComposition,
  deleteComposition,
  followUser,
  getFollowingFeed,
  getHealth,
  getPublicComposition,
  getUserProfile,
  listCompositions,
  listPublicCompositions,
  publishComposition,
  unfollowUser,
  unpublishComposition,
} from "@/lib/api";

// --- Health ------------------------------------------------------------

export function useBackendHealth() {
  return useQuery({
    queryKey: ["health"],
    queryFn: getHealth,
  });
}

// --- Public reads --------------------------------------------------------

export function usePublicCompositions(page = 0, size = 20) {
  return useQuery({
    queryKey: ["publicCompositions", page, size],
    queryFn: () => listPublicCompositions(page, size),
  });
}

export function usePublicComposition(slug: string) {
  return useQuery({
    queryKey: ["publicComposition", slug],
    queryFn: () => getPublicComposition(slug),
  });
}

export function useUserProfile(username: string) {
  return useQuery({
    queryKey: ["userProfile", username],
    queryFn: () => getUserProfile(username),
  });
}

// --- Following feed (auth) ------------------------------------------------

export function useFollowingFeed(username: string | undefined, page = 0, size = 20) {
  return useQuery({
    queryKey: ["followingFeed", username, page, size],
    queryFn: () => getFollowingFeed(page, size),
    enabled: !!username,
  });
}

// --- Owner's compositions (auth) ------------------------------------------

export function useMyCompositions(username: string | undefined) {
  return useQuery({
    queryKey: ["myCompositions", username],
    queryFn: listCompositions,
    enabled: !!username,
  });
}

function useInvalidateMyCompositions() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["myCompositions"] });
}

export function useCreateComposition() {
  const invalidate = useInvalidateMyCompositions();
  return useMutation({
    mutationFn: (body: CompositionInput) => createComposition(body),
    onSuccess: invalidate,
  });
}

export function useDeleteComposition() {
  const invalidate = useInvalidateMyCompositions();
  return useMutation({
    mutationFn: (id: number) => deleteComposition(id),
    onSuccess: invalidate,
  });
}

/** Publishes an unpublished composition, or unpublishes a published one. */
export function useTogglePublish() {
  const invalidate = useInvalidateMyCompositions();
  return useMutation({
    mutationFn: (c: Composition) =>
      c.isPublic ? unpublishComposition(c.id) : publishComposition(c.id),
    onSuccess: invalidate,
  });
}

// --- Follow / unfollow (optimistic) ---------------------------------------

/** Toggles following `username`, updating the cached profile immediately and
 * rolling back if the request fails - mirrors the previous hand-rolled
 * optimistic update exactly, just centralized. */
export function useToggleFollow(username: string) {
  const queryClient = useQueryClient();
  const key = ["userProfile", username];

  return useMutation({
    mutationFn: (next: boolean) => (next ? followUser(username) : unfollowUser(username)),
    onMutate: async (next: boolean) => {
      await queryClient.cancelQueries({ queryKey: key });
      const previous = queryClient.getQueryData<UserProfile>(key);
      if (previous) {
        queryClient.setQueryData<UserProfile>(key, {
          ...previous,
          isFollowing: next,
          followerCount: previous.followerCount + (next ? 1 : -1),
        });
      }
      return { previous };
    },
    onError: (_err, _next, context) => {
      if (context?.previous) queryClient.setQueryData(key, context.previous);
    },
  });
}
