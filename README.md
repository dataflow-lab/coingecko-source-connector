# CoinGecko Kafka Source Connector

A Kafka Connect source connector that polls the [CoinGecko API](https://www.coingecko.com/en/api) for cryptocurrency market data and simple price snapshots, producing Avro-serialized records into Kafka topics.

## Features

- **Two CoinGecko endpoints** — `/coins/markets` (detailed market data) and `/simple/price` (lightweight price snapshots)
- **Avro serialization** with Schema Registry integration
- **Rate limiting** — token-bucket algorithm to stay within CoinGecko API limits
- **Automatic retries** — handles 429, 5xx, and timeout errors with backoff
- **Deduplication** — offset tracking skips records that haven't changed
- **Multi-task scaling** — distributes coins across configurable number of tasks
- **Flexible currency** — quote in USD, EUR, GBP, BTC, or any CoinGecko-supported currency

## Requirements

- Java 17+
- Apache Kafka 3.x with Kafka Connect
- Confluent Schema Registry
- CoinGecko API key ([get one here](https://www.coingecko.com/en/api/pricing))

## Quick Start

### 1. Build the connector plugin

```bash
./gradlew connectPlugin
```

This produces a ready-to-deploy plugin directory at:
```
build/connect-plugin/stitch80-kafka-connect-coingecko/
```

### 2. Start the infrastructure

The included `docker-compose.yml` provides a full local stack: 3-node KRaft Kafka cluster, Schema Registry, Kafka Connect, Conduktor Console, PostgreSQL, Elasticsearch, and Kibana.

```bash
docker compose up -d
```

| Service           | Port  |
|-------------------|-------|
| Kafka brokers     | 19092, 19094, 19096 |
| Schema Registry   | 18081 |
| Kafka Connect     | 18083 |
| Conduktor Console | 18088 |
| PostgreSQL        | 5433  |
| Elasticsearch     | 9200  |
| Kibana            | 5601  |
| Dejavu            | 1358  |

### 3. Deploy the connector

```bash
./deploy-connector.sh
```

This builds the plugin, copies it into the Kafka Connect container, restarts Connect, and verifies the plugin is loaded.

### 4. Register a connector instance

```bash
curl -X POST http://localhost:18083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "coingecko-source",
    "config": {
      "connector.class": "com.stitch80.connect.coingecko.CoinGeckoSourceConnector",
      "tasks.max": "2",
      "coingecko.api.key": "<YOUR_API_KEY>",
      "coingecko.coin.ids": "bitcoin,ethereum,solana",
      "coingecko.endpoints": "markets,simple_price",
      "coingecko.vs.currency": "usd",
      "coingecko.topic.prefix": "raw.coingecko",
      "coingecko.poll.interval.ms": "60000"
    }
  }'
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `coingecko.api.key` | *(required)* | CoinGecko Demo or Pro API key |
| `coingecko.coin.ids` | `bitcoin,ethereum` | Comma-separated coin IDs to track |
| `coingecko.endpoints` | `markets,simple_price` | Endpoints to poll (`markets`, `simple_price`) |
| `coingecko.vs.currency` | `usd` | Quote currency for prices |
| `coingecko.topic.prefix` | `raw.coingecko` | Kafka topic name prefix |
| `coingecko.poll.interval.ms` | `60000` | Poll interval in milliseconds (min: 10000) |
| `coingecko.rate.limit.calls.per.minute` | `25` | API calls per minute (demo tier limit is 30) |
| `coingecko.markets.per.page` | `250` | Results per page for /coins/markets (max: 250) |
| `coingecko.request.timeout.ms` | `30000` | HTTP request timeout in milliseconds (min: 5000) |

## Kafka Topics

Records are produced to topics named by coin and endpoint:

- **Market data** — `{topic.prefix}.markets.{coinId}` (e.g., `raw.coingecko.markets.bitcoin`)
- **Simple price** — `{topic.prefix}.price.{coinId}` (e.g., `raw.coingecko.price.bitcoin`)

Record keys are the coin symbol in uppercase (e.g., `BTC`, `ETH`). Values are Avro-encoded.

### Market Data Schema (`CoinMarketEvent`)

Includes `currentPrice`, `marketCap`, `totalVolume`, `high24h`, `low24h`, price change percentages (1h, 24h, 7d, 14d, 30d), supply data, ATH/ATL, and `ingestionTime`.

### Simple Price Schema (`CoinSimplePriceEvent`)

Includes `price`, `vol24h`, `change24h`, `marketCap`, `lastUpdatedAt`, and `ingestionTime`.

## Managing the Connector

```bash
# Check status
curl http://localhost:18083/connectors/coingecko-source/status

# Pause
curl -X PUT http://localhost:18083/connectors/coingecko-source/pause

# Resume
curl -X PUT http://localhost:18083/connectors/coingecko-source/resume

# Delete
curl -X DELETE http://localhost:18083/connectors/coingecko-source

# View logs
docker logs -f kafka-connect-local
```

## Development

### Project Structure

```
src/main/kotlin/com/stitch80/connect/coingecko/
├── CoinGeckoSourceConnector.kt   # Connector lifecycle & task distribution
├── CoinGeckoSourceTask.kt        # Polling loop & record production
├── CoinGeckoSourceConfig.kt      # Configuration definitions & validation
├── api/
│   ├── CoinGeckoApiClient.kt     # HTTP client with retry logic
│   └── RateLimiter.kt            # Token-bucket rate limiter
├── converter/
│   ├── MarketDataConverter.kt    # CoinMarketData → Kafka Struct
│   └── SimplePriceConverter.kt   # CoinSimplePrice → Kafka Struct
└── model/
    ├── CoinMarketData.kt         # Market data model
    └── CoinSimplePrice.kt        # Simple price model
```

### Running Tests

```bash
./gradlew test
```

Tests use JUnit 5, MockK, and OkHttp MockWebServer for API response simulation.

### Build Tasks

```bash
./gradlew build           # Standard build + tests
./gradlew connectJar      # Fat JAR with all runtime dependencies
./gradlew connectPlugin   # Assembled plugin directory for Kafka Connect
```
