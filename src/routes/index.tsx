import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "AlgoRythm — Hear your code" },
      {
        name: "description",
        content:
          "AlgoRythm is a browser-based tool where code becomes music. Project scaffold — features coming soon.",
      },
    ],
  }),
  component: Index,
});

function Index() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-background px-6 text-center">
      <h1 className="text-5xl font-bold tracking-tight text-foreground sm:text-6xl">
        AlgoRythm
      </h1>
      <p className="mt-4 max-w-xl text-base text-muted-foreground sm:text-lg">
        Where code becomes music. This is the project scaffold — the composer,
        sound engine, and library are being added feature by feature.
      </p>
      <p className="mt-8 text-sm text-muted-foreground">
        Scaffold is running. ✅
      </p>
    </main>
  );
}
