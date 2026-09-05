# Go gRPC example

Implements `hcwebhook.v1.HealthWebhook/Deliver` and returns `ok: true`.

## Requirements

- Go 1.22+
- `protoc`
- Plugins on `PATH`:

```bash
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
```

Ensure `$(go env GOPATH)/bin` is on your `PATH`.

## Run locally

```bash
cd server/example/go
./generate.sh
go mod tidy
go run .
```

Listen: `:50051`  
App target: `http://YOUR_LAN_IP:50051`

## Auth and TLS

This minimal Go sample is plaintext-only and does not enforce `API_KEY`.

For auth + TLS demos, use the [Python](../python/) or [TypeScript](../typescript/) examples (or Docker):

```bash
cd ..
API_KEY=supersecret docker compose --profile python up --build
```

You can extend `main.go` with a metadata interceptor the same way.

## Regenerate stubs

```bash
./generate.sh
```

Output: `gen/` (gitignored).

## What it logs

`timestamp`, `app_version`, and counts for `steps`, `heart_rate`, `sleep`.
