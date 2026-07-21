# Load tests

k6 script covering the core money-movement path through the api-gateway: register + login
(user-service), open account (account-service), deposit + read back status (transaction-service).

## Prerequisites

- All services running (`./gradlew bootRun` per service, or however you normally start the stack)
  and reachable at `http://localhost:8080` via api-gateway.
- Docker, to run the `grafana/k6` image without a local k6 install.

## Running

```bash
docker run --rm -i --network host \
  -e BASE_URL=http://localhost:8080 \
  grafana/k6 run - < load-tests/money-movement.js
```

## Rate limiter caveat

api-gateway applies a per-client-IP rate limiter (`bank-platform.rate-limit.capacity`, default
100 requests / 60s — see `GatewayRoutesConfig`). All k6 traffic originates from a single
container IP, so at more than a handful of VUs you will see `429` responses that are the rate
limiter working as intended, not a service failure. To load test past that ceiling, raise the
limit for the duration of the run:

```bash
RATE_LIMIT_CAPACITY=100000 ./gradlew :api-gateway:bootRun
```
