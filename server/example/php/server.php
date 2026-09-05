<?php
declare(strict_types=1);

/**
 * Minimal HC Webhook gRPC server (plaintext) on 0.0.0.0:50051.
 *
 * Prerequisites:
 *   1. PHP grpc extension enabled (`php -m | grep grpc`)
 *   2. composer install
 *   3. ./generate.sh
 */

require __DIR__ . '/vendor/autoload.php';

use Grpc\RpcServer;
use Hcwebhook\V1\DeliverResponse;
use Hcwebhook\V1\HealthPayload;
use Hcwebhook\V1\HealthWebhookStub;

const PORT = 50051;

if (!extension_loaded('grpc')) {
    fwrite(STDERR, "PHP extension 'grpc' is not loaded.\n");
    fwrite(STDERR, "See README.md — or use the Python/Go/TypeScript examples instead.\n");
    exit(1);
}

if (!class_exists(HealthWebhookStub::class)) {
    fwrite(STDERR, "Generated stubs missing. Run: ./generate.sh\n");
    exit(1);
}

final class HealthWebhookService extends HealthWebhookStub
{
    /** @param HealthPayload $request */
    public function Deliver($request, $context): DeliverResponse
    {
        $steps = method_exists($request, 'getSteps') ? count($request->getSteps()) : 0;
        $hr = method_exists($request, 'getHeartRate') ? count($request->getHeartRate()) : 0;
        $sleep = method_exists($request, 'getSleep') ? count($request->getSleep()) : 0;

        fwrite(
            STDOUT,
            sprintf(
                "Deliver timestamp=%s app_version=%s steps=%d heart_rate=%d sleep=%d\n",
                $request->getTimestamp(),
                $request->getAppVersion(),
                $steps,
                $hr,
                $sleep
            )
        );

        $response = new DeliverResponse();
        $response->setOk(true);
        $response->setMessage('accepted');
        return $response;
    }
}

$server = new RpcServer();
$server->addHttp2Port('0.0.0.0:' . PORT);
$server->handle(new HealthWebhookService());

fwrite(STDOUT, 'HC Webhook gRPC example listening on 0.0.0.0:' . PORT . PHP_EOL);
fwrite(STDOUT, 'App target: http://YOUR_LAN_IP:' . PORT . PHP_EOL);

$server->run();
