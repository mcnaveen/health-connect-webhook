#!/usr/bin/env python3
from concurrent import futures
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "gen"))

import grpc
from hcwebhook.v1 import health_payload_pb2 as pb
from hcwebhook.v1 import health_payload_pb2_grpc as pb_grpc

PORT = int(os.environ.get("PORT", "50051"))
# Optional shared secret. App sends it as a custom header, e.g. x-api-key.
# Leave empty to disable auth (LAN demos).
API_KEY = os.environ.get("API_KEY", "").strip()
API_KEY_METADATA = "x-api-key"


class HealthWebhook(pb_grpc.HealthWebhookServicer):
    def Deliver(self, request, context):
        if API_KEY:
            md = dict(context.invocation_metadata() or [])
            # gRPC metadata keys are lowercased
            got = md.get(API_KEY_METADATA, "")
            if got != API_KEY:
                context.abort(grpc.StatusCode.UNAUTHENTICATED, "invalid or missing x-api-key")

        print(
            "Deliver",
            f"timestamp={request.timestamp}",
            f"app_version={request.app_version}",
            f"steps={len(request.steps)}",
            f"heart_rate={len(request.heart_rate)}",
            f"sleep={len(request.sleep)}",
            flush=True,
        )
        return pb.DeliverResponse(ok=True, message="accepted")


def main() -> None:
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=4))
    pb_grpc.add_HealthWebhookServicer_to_server(HealthWebhook(), server)

    cert_file = os.environ.get("TLS_CERT_FILE", "").strip()
    key_file = os.environ.get("TLS_KEY_FILE", "").strip()
    listen = f"[::]:{PORT}"

    if cert_file and key_file:
        with open(cert_file, "rb") as f:
            cert = f.read()
        with open(key_file, "rb") as f:
            key = f.read()
        creds = grpc.ssl_server_credentials([(key, cert)])
        server.add_secure_port(listen, creds)
        mode = "TLS"
        app_hint = f"https://YOUR_HOST:{PORT}"
    else:
        server.add_insecure_port(listen)
        mode = "plaintext"
        app_hint = f"http://YOUR_LAN_IP:{PORT}"

    server.start()
    print(f"HC Webhook gRPC example listening on {listen} ({mode})", flush=True)
    print(f"App target: {app_hint}", flush=True)
    if API_KEY:
        print(f"Auth: require metadata {API_KEY_METADATA}", flush=True)
    server.wait_for_termination()


if __name__ == "__main__":
    main()
