package com.stitch80.connect.coingecko

import org.apache.kafka.common.config.AbstractConfig
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigException

class CoinGeckoSourceConfig(props: Map<String, String>) : AbstractConfig(CONFIG_DEF, props) {

    fun apiKey(): String = getPassword(API_KEY).value()

    fun baseUrl(): String = "https://api.coingecko.com/api/v3/"

    fun apiKeyHeader(): String = "x-cg-demo-api-key"

    fun coinIds(): List<String> = getString(COIN_IDS).split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun vsCurrency(): String = getString(VS_CURRENCY)

    fun topicPrefix(): String = getString(TOPIC_PREFIX)

    fun pollIntervalMs(): Long = getLong(POLL_INTERVAL_MS)

    fun enabledEndpoints(): Set<String> = getString(ENDPOINTS).split(",").map { it.trim() }.toSet()

    fun marketsPerPage(): Int = getInt(MARKETS_PER_PAGE)

    fun rateLimitCallsPerMinute(): Int = getInt(RATE_LIMIT_CALLS_PER_MINUTE)

    fun requestTimeoutMs(): Long = getLong(REQUEST_TIMEOUT_MS)

    fun taskAssignedCoins(): List<String> =
        getString(TASK_ASSIGNED_COINS)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: coinIds()

    companion object {
        const val API_KEY = "coingecko.api.key"
        const val COIN_IDS = "coingecko.coin.ids"
        const val VS_CURRENCY = "coingecko.vs.currency"
        const val TOPIC_PREFIX = "coingecko.topic.prefix"
        const val POLL_INTERVAL_MS = "coingecko.poll.interval.ms"
        const val ENDPOINTS = "coingecko.endpoints"
        const val MARKETS_PER_PAGE = "coingecko.markets.per.page"
        const val RATE_LIMIT_CALLS_PER_MINUTE = "coingecko.rate.limit.calls.per.minute"
        const val REQUEST_TIMEOUT_MS = "coingecko.request.timeout.ms"
        const val TASK_ASSIGNED_COINS = "coingecko.task.assigned.coins"

        val VALID_ENDPOINTS = setOf("markets", "simple_price")

        val CONFIG_DEF: ConfigDef = ConfigDef()
            .define(
                API_KEY,
                ConfigDef.Type.PASSWORD,
                ConfigDef.NO_DEFAULT_VALUE,
                ConfigDef.Importance.HIGH,
                "CoinGecko Demo API key"
            )
            .define(
                COIN_IDS,
                ConfigDef.Type.STRING,
                "bitcoin,ethereum",
                CoinIdsValidator(),
                ConfigDef.Importance.HIGH,
                "Comma-separated CoinGecko coin IDs to track"
            )
            .define(
                VS_CURRENCY,
                ConfigDef.Type.STRING,
                "usd",
                ConfigDef.Importance.MEDIUM,
                "Quote currency for prices (e.g. usd, eur, btc)"
            )
            .define(
                TOPIC_PREFIX,
                ConfigDef.Type.STRING,
                "raw.coingecko",
                ConfigDef.Importance.MEDIUM,
                "Kafka topic name prefix"
            )
            .define(
                POLL_INTERVAL_MS,
                ConfigDef.Type.LONG,
                60_000L,
                ConfigDef.Range.atLeast(10_000L),
                ConfigDef.Importance.MEDIUM,
                "Poll interval in milliseconds (minimum 10s)"
            )
            .define(
                ENDPOINTS,
                ConfigDef.Type.STRING,
                "markets,simple_price",
                EndpointsValidator(),
                ConfigDef.Importance.MEDIUM,
                "Comma-separated endpoints to poll: markets, simple_price"
            )
            .define(
                MARKETS_PER_PAGE,
                ConfigDef.Type.INT,
                250,
                ConfigDef.Range.between(1, 250),
                ConfigDef.Importance.LOW,
                "Results per page for /coins/markets (max 250)"
            )
            .define(
                RATE_LIMIT_CALLS_PER_MINUTE,
                ConfigDef.Type.INT,
                25,
                ConfigDef.Range.between(1, 1000),
                ConfigDef.Importance.MEDIUM,
                "Max API calls per minute (demo tier limit is 30)"
            )
            .define(
                REQUEST_TIMEOUT_MS,
                ConfigDef.Type.LONG,
                30_000L,
                ConfigDef.Range.atLeast(5_000L),
                ConfigDef.Importance.LOW,
                "HTTP request timeout in milliseconds"
            )
            .define(
                TASK_ASSIGNED_COINS,
                ConfigDef.Type.STRING,
                null,
                ConfigDef.Importance.LOW,
                "Internal: coin IDs assigned to this task (set by connector)"
            )
    }

    class CoinIdsValidator : ConfigDef.Validator {
        override fun ensureValid(name: String, value: Any?) {
            val str = value as? String ?: throw ConfigException(name, value, "must be a non-empty string")
            val ids = str.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (ids.isEmpty()) {
                throw ConfigException(name, value, "must contain at least one coin ID")
            }
        }
    }

    class EndpointsValidator : ConfigDef.Validator {
        override fun ensureValid(name: String, value: Any?) {
            val str = value as? String ?: throw ConfigException(name, value, "must be a non-empty string")
            val endpoints = str.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (endpoints.isEmpty()) {
                throw ConfigException(name, value, "must contain at least one endpoint")
            }
            val invalid = endpoints.filterNot { it in VALID_ENDPOINTS }
            if (invalid.isNotEmpty()) {
                throw ConfigException(name, value, "invalid endpoints: $invalid. Valid: $VALID_ENDPOINTS")
            }
        }
    }
}
