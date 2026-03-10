package com.stitch80.connect.coingecko

import com.stitch80.connect.coingecko.api.CoinGeckoApiClient
import com.stitch80.connect.coingecko.api.RateLimiter
import com.stitch80.connect.coingecko.converter.MarketDataConverter
import com.stitch80.connect.coingecko.converter.SimplePriceConverter
import mu.KotlinLogging
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.errors.RetriableException
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask

private val logger = KotlinLogging.logger {}

class CoinGeckoSourceTask : SourceTask() {

    private lateinit var config: CoinGeckoSourceConfig
    private lateinit var apiClient: CoinGeckoApiClient
    private lateinit var rateLimiter: RateLimiter
    private var assignedCoinIds: List<String> = emptyList()
    private var enabledEndpoints: Set<String> = emptySet()

    override fun version(): String = CoinGeckoSourceConnector.VERSION

    override fun start(props: Map<String, String>) {
        config = CoinGeckoSourceConfig(props)
        assignedCoinIds = config.taskAssignedCoins()
        enabledEndpoints = config.enabledEndpoints()

        apiClient = CoinGeckoApiClient(
            baseUrl = config.baseUrl(),
            apiKey = config.apiKey(),
            apiKeyHeader = config.apiKeyHeader(),
            timeoutMs = config.requestTimeoutMs(),
        )
        rateLimiter = RateLimiter(config.rateLimitCallsPerMinute())

        logger.info {
            "CoinGecko task started: coins=$assignedCoinIds, endpoints=$enabledEndpoints"
        }
    }

    override fun poll(): List<SourceRecord>? {
        val records = mutableListOf<SourceRecord>()
        val ingestionTime = System.currentTimeMillis()

        try {
            if ("markets" in enabledEndpoints) {
                records.addAll(pollMarkets(ingestionTime))
            }
            if ("simple_price" in enabledEndpoints) {
                records.addAll(pollSimplePrice(ingestionTime))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during poll cycle" }
            if (isRetriable(e)) {
                throw RetriableException("Retriable error during CoinGecko poll", e)
            }
        }

        if (records.isNotEmpty()) {
            logger.info { "Polled ${records.size} records from CoinGecko" }
        }

        Thread.sleep(config.pollIntervalMs())
        return records
    }

    private fun pollMarkets(ingestionTime: Long): List<SourceRecord> {
        rateLimiter.acquire()

        val marketData = apiClient.fetchMarkets(
            coinIds = assignedCoinIds,
            vsCurrency = config.vsCurrency(),
            perPage = config.marketsPerPage(),
        )

        return marketData.mapNotNull { coin ->
            val partition = sourcePartition(coin.id, "markets")
            val storedOffset = context.offsetStorageReader().offset(partition)
            val lastUpdated = storedOffset?.get("lastUpdated") as? String

            if (lastUpdated != null && lastUpdated == coin.lastUpdated) {
                logger.debug { "Skipping ${coin.id} markets (unchanged: $lastUpdated)" }
                return@mapNotNull null
            }

            val offset = mapOf("lastUpdated" to coin.lastUpdated)
            val topic = "${config.topicPrefix()}.markets.${coin.id}"

            SourceRecord(
                partition,
                offset,
                topic,
                Schema.STRING_SCHEMA,
                coin.symbol.uppercase(),
                MarketDataConverter.SCHEMA,
                MarketDataConverter.toStruct(coin, ingestionTime),
            )
        }
    }

    private fun pollSimplePrice(ingestionTime: Long): List<SourceRecord> {
        rateLimiter.acquire()

        val prices = apiClient.fetchSimplePrice(
            coinIds = assignedCoinIds,
            vsCurrency = config.vsCurrency(),
        )

        return prices.mapNotNull { (coinId, priceData) ->
            val lastUpdatedAt = priceData.lastUpdatedAt ?: return@mapNotNull null

            val partition = sourcePartition(coinId, "simple_price")
            val storedOffset = context.offsetStorageReader().offset(partition)
            val lastKnown = storedOffset?.get("lastUpdatedAt") as? Long

            if (lastKnown != null && lastKnown >= lastUpdatedAt) {
                logger.debug { "Skipping $coinId price (unchanged: $lastUpdatedAt)" }
                return@mapNotNull null
            }

            val offset = mapOf("lastUpdatedAt" to lastUpdatedAt)
            val topic = "${config.topicPrefix()}.price.$coinId"

            SourceRecord(
                partition,
                offset,
                topic,
                Schema.STRING_SCHEMA,
                coinId.uppercase(),
                SimplePriceConverter.SCHEMA,
                SimplePriceConverter.toStruct(priceData, ingestionTime),
            )
        }
    }

    private fun sourcePartition(coinId: String, endpoint: String): Map<String, String> = mapOf(
        "source" to "coingecko",
        "coinId" to coinId,
        "endpoint" to endpoint,
    )

    private fun isRetriable(e: Exception): Boolean =
        e is java.net.SocketTimeoutException ||
            e is java.net.ConnectException ||
            e.message?.contains("429") == true ||
            e.message?.contains("5") == true && e.message?.contains("HTTP") == true

    override fun stop() {
        logger.info { "CoinGecko task stopping" }
        if (::apiClient.isInitialized) apiClient.close()
        if (::rateLimiter.isInitialized) rateLimiter.close()
    }
}
