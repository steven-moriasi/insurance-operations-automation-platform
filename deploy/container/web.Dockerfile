FROM node:22.20.0-alpine AS build

WORKDIR /workspace
COPY apps/web/package.json apps/web/package-lock.json ./
RUN npm ci
COPY apps/web ./
RUN npm run build

FROM node:22.20.0-alpine

WORKDIR /app
COPY deploy/container/web-server.mjs ./
COPY --from=build /workspace/dist ./public
USER node
EXPOSE 8080
ENTRYPOINT ["node", "/app/web-server.mjs"]
