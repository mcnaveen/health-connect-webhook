# Build from repo root (docker-compose sets context: ../..)
FROM node:22-bookworm-slim

WORKDIR /app

COPY server/example/typescript/package.json /app/package.json
RUN npm install

COPY proto /proto
COPY server/example/typescript /app

ENV PORT=50051
EXPOSE 50051

CMD ["npm", "start"]
