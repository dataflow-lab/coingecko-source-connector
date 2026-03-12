#!/bin/bash
# Build the CoinGecko source connector fat JAR, copy it into the kafka-connect
# Docker container, restart Connect, and verify the plugin is loaded.
#
# Usage: ./deploy-connector.sh [CONNECT_CONTAINER] [CONNECT_HOST]
#   CONNECT_CONTAINER  defaults to kafka-connect-local
#   CONNECT_HOST       defaults to localhost:18083
#
# Prerequisites:
#   - The kafka-connect container must be running
#
# After this script finishes, register the connector manually:
#   devops/docker/connect/register-coingecko-source.sh

set -euo pipefail

CONNECT_CONTAINER="${1:-kafka-connect-local}"
CONNECT_HOST="${2:-localhost:18083}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGIN_NAME="stitch80-kafka-connect-coingecko"
PLUGIN_DIR="/usr/share/confluent-hub-components/$PLUGIN_NAME"

# ── 1. Build ────────────────────────────────────────────────────────────────
echo "==> Building connector plugin..."
cd "$SCRIPT_DIR"
./gradlew clean connectPlugin --quiet

LOCAL_PLUGIN="$SCRIPT_DIR/build/connect-plugin/$PLUGIN_NAME"
if [ ! -d "$LOCAL_PLUGIN" ]; then
  echo "ERROR: Plugin directory not found at $LOCAL_PLUGIN"
  exit 1
fi
echo "    Built plugin contents:"
ls -lh "$LOCAL_PLUGIN"

# ── 2. Verify container is running ──────────────────────────────────────────
if ! docker inspect -f '{{.State.Running}}' "$CONNECT_CONTAINER" 2>/dev/null | grep -q true; then
  echo "ERROR: Container '$CONNECT_CONTAINER' is not running."
  echo "       Start the stack first: docker compose -f devops/docker/full-stack.yaml up -d"
  exit 1
fi

# ── 3. Copy plugin into container ─────────────────────────────────────────
echo "==> Removing old plugin from container (if any)..."
docker exec "$CONNECT_CONTAINER" rm -rf "$PLUGIN_DIR" 2>/dev/null || true

echo "==> Copying plugin directory to $CONNECT_CONTAINER:$PLUGIN_DIR/"
docker cp "$LOCAL_PLUGIN" "$CONNECT_CONTAINER:$PLUGIN_DIR"

echo "==> Verifying plugin in container..."
docker exec "$CONNECT_CONTAINER" ls -lhR "$PLUGIN_DIR"

# ── 4. Restart Kafka Connect to pick up the new plugin ──────────────────────
echo "==> Restarting Kafka Connect to load the new plugin..."
docker restart "$CONNECT_CONTAINER"

echo "==> Waiting for Kafka Connect to be ready..."
until curl -s -o /dev/null -w "%{http_code}" "http://${CONNECT_HOST}/connectors" 2>/dev/null | grep -q "200"; do
    echo "    Not ready yet, retrying in 5s..."
    sleep 5
done
echo "    Kafka Connect is ready."

# ── 5. Verify plugin is loaded ──────────────────────────────────────────────
echo "==> Checking installed connector plugins..."
if curl -s "http://${CONNECT_HOST}/connector-plugins" 2>/dev/null | grep -q "CoinGeckoSourceConnector"; then
  echo "    ✓ CoinGeckoSourceConnector plugin found"
else
  echo "    ✗ CoinGeckoSourceConnector NOT found in plugin list."
  echo "      Available plugins:"
  curl -s "http://${CONNECT_HOST}/connector-plugins" | python3 -m json.tool 2>/dev/null || \
  curl -s "http://${CONNECT_HOST}/connector-plugins"
  echo ""
  echo "      Check container logs: docker logs $CONNECT_CONTAINER"
  exit 1
fi

echo ""
echo "=== Deployment complete ==="
echo "  Container:  $CONNECT_CONTAINER"
echo "  Plugin dir: $PLUGIN_DIR"
echo "  Connect:    http://$CONNECT_HOST"
echo ""
echo "Next step — register the connector:"
echo "  ./devops/docker/connect/register-coingecko-source.sh $CONNECT_HOST"
echo ""
echo "Useful commands:"
echo "  Status:     curl -s http://$CONNECT_HOST/connectors/coingecko-source/status | python3 -m json.tool"
echo "  Pause:      curl -X PUT http://$CONNECT_HOST/connectors/coingecko-source/pause"
echo "  Resume:     curl -X PUT http://$CONNECT_HOST/connectors/coingecko-source/resume"
echo "  Delete:     curl -X DELETE http://$CONNECT_HOST/connectors/coingecko-source"
echo "  Logs:       docker logs -f $CONNECT_CONTAINER"
