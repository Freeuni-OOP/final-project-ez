// Save / load + publish panel for the composer. Only shows when signed in. Saves
// the current pattern + BPM under a title, lists the user's saved compositions,
// loads or deletes them, and publishes/unpublishes (with a copyable share link).


import { useState } from "react";


import { type Composition } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import {
 useCreateComposition,
 useDeleteComposition,
 useMyCompositions,
 useTogglePublish,
} from "@/lib/queries";


export function CompositionBar({
 source,
 bpm,
 onLoad,
}: {
 source: string;
 bpm: number;
 onLoad: (pattern: string, bpm: number) => void;
}) {
 const { user } = useAuth();
 const { t } = useI18n();


 const { data: items = [] } = useMyCompositions(user?.username);
 const createMutation = useCreateComposition();
 const deleteMutation = useDeleteComposition();
 const toggleMutation = useTogglePublish();


 const [title, setTitle] = useState("");
 const [error, setError] = useState<string | null>(null);
 const [copiedId, setCopiedId] = useState<number | null>(null);


 if (!user) {
   return (
     <div className="rounded-xl border border-border bg-foreground/5 p-4 text-sm text-muted-foreground">
       {t("login_to_save")}
     </div>
   );
 }


 const save = async () => {
   setError(null);
   if (!title.trim()) {
     setError(t("title_required"));
     return;
   }
   try {
     await createMutation.mutateAsync({ title: title.trim(), pattern: source, bpm });
     setTitle("");
   } catch (e) {
     setError(e instanceof Error ? e.message : "Save failed.");
   }
 };


 const remove = (id: number) => {
   // fire-and-forget, same as before — the list stays in sync via the
   // mutation's own cache invalidation, no explicit refresh needed
   deleteMutation.mutate(id);
 };


 const togglePublish = (c: Composition) => {
   toggleMutation.mutate(c, {
     onError: (e) => setError(e instanceof Error ? e.message : "Could not update."),
   });
 };


 const copyLink = async (c: Composition) => {
   if (!c.slug) return;
   try {
     await navigator.clipboard.writeText(`${window.location.origin}/c/${c.slug}`);
     setCopiedId(c.id);
     window.setTimeout(() => setCopiedId((id) => (id === c.id ? null : id)), 1500);
   } catch {
     // clipboard unavailable — ignore
   }
 };


 return (
   <div className="rounded-xl border border-border bg-foreground/5 p-4">
     <p className="mb-3 text-sm font-medium">{t("my_compositions")}</p>


     <div className="flex gap-2">
       <input
         value={title}
         onChange={(e) => setTitle(e.target.value)}
         placeholder={t("composition_title")}
         className="flex-1 rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
       />
       <button
         onClick={save}
         disabled={createMutation.isPending}
         className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60"
       >
         {t("save")}
       </button>
     </div>


     {error && <p className="mt-2 text-xs text-destructive">{error}</p>}


     {items.length > 0 && (
       <ul className="mt-3 space-y-2">
         {items.map((c) => (
           <li key={c.id} className="rounded-md border border-border px-3 py-2">
             <div className="flex items-center justify-between gap-2">
               <span className="truncate text-sm text-foreground">
                 {c.title}
                 <span className="ml-1 text-xs text-muted-foreground">
                   · {c.bpm} {t("bpm")}
                 </span>
               </span>
               <span className="flex shrink-0 gap-3 text-xs">
                 <button
                   onClick={() => onLoad(c.pattern, c.bpm)}
                   className="font-medium text-primary hover:underline"
                 >
                   {t("load")}
                 </button>
                 <button
                   onClick={() => togglePublish(c)}
                   className="text-muted-foreground transition-colors hover:text-foreground"
                 >
                   {c.isPublic ? t("unpublish") : t("publish")}
                 </button>
                 <button
                   onClick={() => remove(c.id)}
                   className="text-muted-foreground transition-colors hover:text-destructive"
                 >
                   {t("delete")}
                 </button>
               </span>
             </div>


             {c.isPublic && c.slug && (
               <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                 <code className="truncate rounded bg-foreground/10 px-1.5 py-0.5">
                   /c/{c.slug}
                 </code>
                 <button onClick={() => copyLink(c)} className="text-primary hover:underline">
                   {copiedId === c.id ? t("link_copied") : t("copy_link")}
                 </button>
               </div>
             )}
           </li>
         ))}
       </ul>
     )}
   </div>
 );
}



