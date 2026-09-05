import * as fs from "node:fs";
import * as path from "node:path";
import * as grpc from "@grpc/grpc-js";
import * as protoLoader from "@grpc/proto-loader";

const PORT = Number(process.env.PORT ?? "50051");
const API_KEY = (process.env.API_KEY ?? "").trim();
const API_KEY_METADATA = "x-api-key";
const TLS_CERT_FILE = (process.env.TLS_CERT_FILE ?? "").trim();
const TLS_KEY_FILE = (process.env.TLS_KEY_FILE ?? "").trim();
/** When "1"/"true", also print full JSON to the console (can be huge). */
const LOG_FULL_JSON = ["1", "true", "yes"].includes(
  (process.env.LOG_FULL_JSON ?? "").trim().toLowerCase(),
);

const PROTO_PATH = path.resolve(
  process.cwd(),
  "../../../proto/hcwebhook/v1/health_payload.proto",
);
const RECEIVED_DIR = path.resolve(process.cwd(), "received");

type DeliverRequest = {
  timestamp?: string;
  app_version?: string;
  steps?: unknown[];
  heart_rate?: unknown[];
  sleep?: unknown[];
  [key: string]: unknown;
};

type DeliverResponse = {
  ok: boolean;
  message: string;
};

type HealthWebhookHandlers = {
  Deliver: (
    call: grpc.ServerUnaryCall<DeliverRequest, DeliverResponse>,
    callback: grpc.sendUnaryData<DeliverResponse>,
  ) => void;
};

const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true,
});

const proto = grpc.loadPackageDefinition(packageDefinition) as {
  hcwebhook: {
    v1: {
      HealthWebhook: {
        service: grpc.ServiceDefinition;
      };
    };
  };
};

function readMetadata(call: grpc.ServerUnaryCall<DeliverRequest, DeliverResponse>, key: string): string {
  const values = call.metadata.get(key);
  if (!values.length) return "";
  const first = values[0];
  return typeof first === "string" ? first : first.toString("utf8");
}

function deliver(
  call: grpc.ServerUnaryCall<DeliverRequest, DeliverResponse>,
  callback: grpc.sendUnaryData<DeliverResponse>,
) {
  if (API_KEY) {
    const got = readMetadata(call, API_KEY_METADATA);
    if (got !== API_KEY) {
      callback({
        code: grpc.status.UNAUTHENTICATED,
        message: "invalid or missing x-api-key",
      });
      return;
    }
  }

  const req = call.request;
  console.log(
    "Deliver",
    `timestamp=${req.timestamp ?? ""}`,
    `app_version=${req.app_version ?? ""}`,
    `steps=${req.steps?.length ?? 0}`,
    `heart_rate=${req.heart_rate?.length ?? 0}`,
    `sleep=${req.sleep?.length ?? 0}`,
  );

  const savedPath = savePayload(req);
  if (savedPath) {
    console.log(`  saved ${savedPath}`);
  }
  if (LOG_FULL_JSON) {
    console.log(JSON.stringify(req, null, 2));
  }

  callback(null, { ok: true, message: "accepted" });
}

function savePayload(req: DeliverRequest): string | null {
  try {
    fs.mkdirSync(RECEIVED_DIR, { recursive: true });
    const stamp = (req.timestamp ?? new Date().toISOString()).replace(/[:.]/g, "-");
    const filePath = path.join(RECEIVED_DIR, `deliver-${stamp}.json`);
    fs.writeFileSync(filePath, JSON.stringify(req, null, 2), "utf8");
    return filePath;
  } catch (err) {
    console.error("  failed to save payload:", err);
    return null;
  }
}

function main() {
  if (!fs.existsSync(PROTO_PATH)) {
    console.error(`Proto not found at ${PROTO_PATH}`);
    console.error("Run this example from server/example/typescript (npm start).");
    process.exit(1);
  }

  const server = new grpc.Server();
  const handlers: HealthWebhookHandlers = { Deliver: deliver };
  server.addService(proto.hcwebhook.v1.HealthWebhook.service, handlers);

  const addr = `0.0.0.0:${PORT}`;
  const credentials =
    TLS_CERT_FILE && TLS_KEY_FILE
      ? grpc.ServerCredentials.createSsl(null, [
          {
            cert_chain: fs.readFileSync(TLS_CERT_FILE),
            private_key: fs.readFileSync(TLS_KEY_FILE),
          },
        ])
      : grpc.ServerCredentials.createInsecure();

  const mode = TLS_CERT_FILE && TLS_KEY_FILE ? "TLS" : "plaintext";
  const appHint =
    mode === "TLS" ? `https://YOUR_HOST:${PORT}` : `http://YOUR_LAN_IP:${PORT}`;

  server.bindAsync(addr, credentials, (err) => {
    if (err) {
      console.error(err);
      process.exit(1);
    }
    console.log(`HC Webhook gRPC example listening on ${addr} (${mode})`);
    console.log(`App target: ${appHint}`);
    console.log(`Payloads saved under: ${RECEIVED_DIR}`);
    if (API_KEY) {
      console.log(`Auth: require metadata ${API_KEY_METADATA}`);
    }
    if (LOG_FULL_JSON) {
      console.log("LOG_FULL_JSON=1 — full JSON will also print to the console");
    }
  });
}

main();
