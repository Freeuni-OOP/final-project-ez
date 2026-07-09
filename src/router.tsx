import { QueryClient } from "@tanstack/react-query";
import { createRouter } from "@tanstack/react-router";
import { routeTree } from "./routeTree.gen";


export const getRouter = () => {
 // No retries: a failed request (e.g. a 404 on a bad slug) should surface
 // immediately, not after a few silent, backed-off attempts.
 const queryClient = new QueryClient({
   defaultOptions: { queries: { retry: false } },
 });


 const router = createRouter({
   routeTree,
   context: { queryClient },
   scrollRestoration: true,
   defaultPreloadStaleTime: 0,
 });


 return router;
};



