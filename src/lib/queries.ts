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
  getUnreadNotificationCount,
  getUserProfile,
  listCompositions,
  listNotifications,
  listPublicCompositions,
  markNotificationRead,
  remixComposition,
  searchPublicCompositions,
  publishComposition,
  unfollowUser,
  unpublishComposition,
  updateComposition,
  reportComposition,
  reportComment,
  getAdminReports,
  adminRemoveComposition,
  adminRemoveComment,
  dismissReport,
  getSiteStats,
} from "@/lib/api";

// --- Health ------------------------------------------------------------

export function useBackendHealth() {
  return useQuery({
    queryKey: ["health"],
    queryFn: getHealth,
  });
}

// --- Public reads --------------------------------------------------------

export function usePublicCompositions(page = 0, size = 20, tag?: string, search = "") {
  return useQuery({
    queryKey: ["publicCompositions", page, size, tag ?? "", search],
    queryFn: () =>
      search.trim()
        ? searchPublicCompositions(search.trim())
        : listPublicCompositions(page, size, tag),
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
export function useRemixComposition() {
  const invalidate = useInvalidateMyCompositions();

  return useMutation({
    mutationFn: (slug: string) => remixComposition(slug),
    onSuccess: invalidate,
  });
}
export function useUpdateComposition() {
  const invalidate = useInvalidateMyCompositions();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: CompositionInput }) =>
      updateComposition(id, body),
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

// --- Notifications (auth) --------------------------------------------------

// Polled rather than pushed - there's no websocket/SSE channel here, so a
// 30s interval keeps the bell reasonably fresh without hammering the API.
const NOTIFICATIONS_POLL_MS = 30_000;

export function useNotifications(username: string | undefined) {
  return useQuery({
    queryKey: ["notifications", username],
    queryFn: () => listNotifications(),
    enabled: !!username,
    refetchInterval: NOTIFICATIONS_POLL_MS,
  });
}

export function useUnreadNotificationCount(username: string | undefined) {
  return useQuery({
    queryKey: ["notificationsUnreadCount", username],
    queryFn: getUnreadNotificationCount,
    enabled: !!username,
    refetchInterval: NOTIFICATIONS_POLL_MS,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => markNotificationRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
      queryClient.invalidateQueries({ queryKey: ["notificationsUnreadCount"] });
    },
  });
}

// --- Reporting (auth) -----------------------------------------------------

export function useReportComposition() {
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) => reportComposition(id, reason),
  });
}

export function useReportComment() {
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) => reportComment(id, reason),
  });
}

// --- Admin / moderation (admin only) --------------------------------------

export function useAdminReports(enabled: boolean) {
  return useQuery({
    queryKey: ["adminReports"],
    queryFn: getAdminReports,
    enabled,
  });
}

export function useSiteStats(enabled: boolean) {
  return useQuery({
    queryKey: ["siteStats"],
    queryFn: getSiteStats,
    enabled,
  });
}

function useInvalidateAdmin() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["adminReports"] });
    queryClient.invalidateQueries({ queryKey: ["siteStats"] });
  };
}

export function useAdminRemoveComposition() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: (id: number) => adminRemoveComposition(id),
    onSuccess: invalidate,
  });
}

export function useAdminRemoveComment() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: (id: number) => adminRemoveComment(id),
    onSuccess: invalidate,
  });
}

export function useDismissReport() {
  const invalidate = useInvalidateAdmin();
  return useMutation({
    mutationFn: (id: number) => dismissReport(id),
    onSuccess: invalidate,
  });
}
