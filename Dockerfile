# AlgoRythm frontend — TanStack Start (React 19, SSR) on Bun.
FROM oven/bun:1.3 AS app

WORKDIR /app

# Install dependencies first (better layer caching).
COPY package.json bun.lock bunfig.toml ./
RUN bun install --frozen-lockfile

# App source.
COPY . .

# Vite dev server (SSR + HMR). Bound to 0.0.0.0 so it's reachable from the host.
EXPOSE 3000
CMD ["bun", "run", "dev", "--host", "0.0.0.0", "--port", "3000"]
