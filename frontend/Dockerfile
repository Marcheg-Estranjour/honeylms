# Production image of the Angular front (S10-5).
# Stage 1 builds the app with Node; stage 2 serves the static files with an UNPRIVILEGED nginx
# (non-root user, port 8080) that also relays /api to the backend on the same origin (no CORS).

# --- Build stage ---
FROM node:24-alpine AS build
WORKDIR /app

# Dependency layer cached as long as package*.json do not change
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# --- Runtime stage ---
FROM nginxinc/nginx-unprivileged:1.28-alpine

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 8080
