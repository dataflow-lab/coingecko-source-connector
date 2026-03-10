package com.stitch80.connect.coingecko.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.stitch80.connect.coingecko.model.CoinMarketData
import com.stitch80.connect.coingecko.model.CoinSimplePrice
import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Closeable
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class CoinGeckoApiClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val apiKeyHeader: String,
    timeoutMs: Long,
) : Closeable {

    private val objectMapper = jacksonObjectMapper()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build()

    fun fetchMarkets(
        coinIds: List<String>,
        vsCurrency: String,
        perPage: Int,
    ): List<CoinMarketData> {
        val url = "${baseUrl}coins/markets".toHttpUrl().newBuilder()
            .addQueryParameter("vs_currency", vsCurrency)
            .addQueryParameter("ids", coinIds.joinToString(","))
            .addQueryParameter("order", "market_cap_desc")
            .addQueryParameter("per_page", perPage.toString())
            .addQueryParameter("page", "1")
            .addQueryParameter("sparkline", "false")
            .addQueryParameter("price_change_percentage", "1h,24h,7d,14d,30d")
            .build()

        val request = Request.Builder()
            .url(url)
            .header(apiKeyHeader, apiKey)
            .get()
            .build()

        return executeWithRetry(request) { body ->
            objectMapper.readValue(body)
        }
    }

    fun fetchSimplePrice(
        coinIds: List<String>,
        vsCurrency: String,
    ): Map<String, CoinSimplePrice> {
        val url = "${baseUrl}simple/price".toHttpUrl().newBuilder()
            .addQueryParameter("ids", coinIds.joinToString(","))
            .addQueryParameter("vs_currencies", vsCurrency)
            .addQueryParameter("include_24hr_vol", "true")
            .addQueryParameter("include_24hr_change", "true")
            .addQueryParameter("include_market_cap", "true")
            .addQueryParameter("include_last_updated_at", "true")
            .build()

        val request = Request.Builder()
            .url(url)
            .header(apiKeyHeader, apiKey)
            .get()
            .build()

        return executeWithRetry(request) { body ->
            val rawMap: Map<String, Map<String, Any?>> = objectMapper.readValue(body)
            rawMap.mapValues { (coinId, data) ->
                CoinSimplePrice(
                    coinId = coinId,
                    price = (data["${vsCurrency}"] as? Number)?.toDouble(),
                    vol24h = (data["${vsCurrency}_24h_vol"] as? Number)?.toDouble(),
                    change24h = (data["${vsCurrency}_24h_change"] as? Number)?.toDouble(),
                    marketCap = (data["${vsCurrency}_market_cap"] as? Number)?.toDouble(),
                    lastUpdatedAt = (data["last_updated_at"] as? Number)?.toLong(),
                )
            }
        }
    }

    private fun <T> executeWithRetry(request: Request, parse: (String) -> T): T {
        var lastException: Exception? = null
        repeat(3) { attempt ->
            try {
                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    when {
                        resp.isSuccessful -> {
                            val body = resp.body?.string()
                                ?: throw RuntimeException("Empty response body")
                            return parse(body)
                        }
                        resp.code == 429 -> {
                            val retryAfter = resp.header("Retry-After")?.toLongOrNull() ?: 60
                            logger.warn { "Rate limited (429). Waiting ${retryAfter}s before retry..." }
                            Thread.sleep(retryAfter * 1000)
                        }
                        resp.code in 500..599 -> {
                            logger.warn { "Server error ${resp.code}, attempt ${attempt + 1}/3" }
                            Thread.sleep(1000L * (attempt + 1))
                        }
                        else -> {
                            val errorBody = resp.body?.string() ?: "no body"
                            throw RuntimeException("HTTP ${resp.code}: $errorBody")
                        }
                    }
                }
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                logger.warn { "Request failed (attempt ${attempt + 1}/3): ${e.message}" }
                if (attempt < 2) Thread.sleep(1000L * (attempt + 1))
            }
        }
        throw lastException ?: RuntimeException("Failed after 3 retries")
    }

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }
}
