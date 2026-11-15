# syntax=docker/dockerfile:1

# ---- Build stage: compile the React app into static files (dist/) ----
FROM node:24-alpine AS build
RUN npm install -g pnpm@12.3.4
WORKDIR /build

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./

RUN --mount=type=cache,target=/pnpm/store \
    pnpm install --frozen-lockfile --store-dir /pnpm/store

COPY . .
RUN pnpm build

# ---- Runtime stage: nginx serving the static files and forwarding /api to the backend ----
FROM nginxinc/nginx-unprivileged:1.30-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 8080
