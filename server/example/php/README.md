# PHP gRPC example

Implements `hcwebhook.v1.HealthWebhook/Deliver` and returns `ok: true`.

PHP gRPC needs the **gRPC PHP extension** plus generated stubs. That setup is heavier than Python/TypeScript. Prefer those for first-time LAN tests.

## Requirements

- PHP 8.1+
- Composer
- `protoc`
- PHP `grpc` extension ([upstream install notes](https://github.com/grpc/grpc/tree/master/src/php))
- `protoc-gen-php-grpc` on `PATH`

Check:

```bash
php -m | grep grpc
```

## Generate stubs

```bash
cd server/example/php
composer install
./generate.sh
```

Stubs are written under `gen/`.

## Run

```bash
cd server/example/php
php server.php
```

Listen: `0.0.0.0:50051`  
App target: `http://YOUR_LAN_IP:50051`

## Auth and TLS

This minimal PHP sample is plaintext-only. For auth + TLS, use [Python](../python/) or [TypeScript](../typescript/) (or Docker).

## If install fails

Use:

```bash
cd ..
docker compose --profile python up --build
```

## What it logs

`timestamp`, `app_version`, and counts for `steps`, `heart_rate`, `sleep`.
