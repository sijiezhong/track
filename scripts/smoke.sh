#!/usr/bin/env bash
set -euo pipefail

echo "Waiting for server to be healthy..."
for i in {1..60}; do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null; then
    echo "Server is up"
    break
  fi
  sleep 2
done

echo "Hit pixel endpoint to create an event..."
curl -fsS "http://localhost:8080/api/pixel.gif?eventName=smoke&sessionId=smoke-1" -H "X-Tenant-Id: 1" >/dev/null

echo "Query trend analytics..."
curl -fsS "http://localhost:8080/api/events/trend?eventName=smoke&interval=daily" -H "X-Tenant-Id: 1"
echo


