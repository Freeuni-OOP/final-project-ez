# AlgoRythm

**Where code becomes music.** A browser-based tool where you write simple text
patterns and hear them play through drum and synth engines.

> FreeUni OOP final project. The assignment specification is in
> [`34QuizWebsite.pdf`](./34QuizWebsite.pdf).

This repository is built up **incrementally** — each feature lands as its own
reviewed Pull Request. This commit is the **scaffold seed**: a minimal, runnable
base (tooling + app shell + UI primitives). Features (i18n, theming, the app
header, the landing hero, the audio engine, the sound visualizer, the library
and syntax pages) and the Spring Boot backend are introduced in subsequent PRs.

## Tech stack

**Frontend:** TanStack Start v1 (React 19, TypeScript, SSR), Vite 8, Tailwind
CSS v4, shadcn/Radix UI, Three.js, Bun.

**Backend (planned):** Spring Boot 3 (Java 21), Spring Web, Spring Security +
JWT, Spring Data JPA / Hibernate, PostgreSQL, Flyway, Maven, Docker.

## Run with Docker (recommended)

One command brings the project up:

```bash
docker compose up --build
```

Then open <http://localhost:3000>.

As the backend lands, new services (Spring Boot + PostgreSQL) are added to
`docker-compose.yml`, so this same command will spin up the full stack.

## Run locally with Bun

Requires [Bun](https://bun.sh) (v1.3+).

```bash
bun install        # install dependencies
bun run dev        # start the dev server (SSR + HMR) at http://localhost:3000
```

Other scripts:

```bash
bun run build      # production build (client + SSR) into dist/
bun run preview    # preview the production client build
bun run lint       # eslint
bun run format     # prettier --write
```

## Project structure

```
src/
  routes/          # file-based routes (TanStack Router)
    __root.tsx     # root layout, error + 404 boundaries, document shell
    index.tsx      # landing page
  components/ui/   # shadcn/Radix UI primitives
  hooks/           # reusable hooks
  lib/             # utilities + error handling
  router.tsx       # router factory
  server.ts        # SSR entry with error wrapper
  start.ts         # TanStack Start instance + middleware
  styles.css       # Tailwind v4 + design tokens
public/            # static assets (fonts, icons)
```
