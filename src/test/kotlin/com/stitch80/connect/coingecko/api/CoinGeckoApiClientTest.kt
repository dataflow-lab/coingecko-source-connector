package com.stitch80.connect.coingecko.api

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CoinGeckoApiClientTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var client: CoinGeckoApiClient

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()

        client = CoinGeckoApiClient(
            baseUrl = mockServer.url("/api/v3/").toString(),
            apiKey = "test-key",
            apiKeyHeader = "x-cg-demo-api-key",
            timeoutMs = 5000,
        )
    }

    @AfterEach
    fun tearDown() {
        client.close()
        mockServer.shutdown()
    }

    @Test
    fun `fetchMarkets should parse response correctly`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(MARKETS_RESPONSE)
        )

        val result = client.fetchMarkets(listOf("bitcoin"), "usd", 250)

        assertEquals(1, result.size)
        val btc = result[0]
        assertEquals("bitcoin", btc.id)
        assertEquals("btc", btc.symbol)
        assertEquals("Bitcoin", btc.name)
        assertEquals(50000.0, btc.currentPrice)
        assertEquals(950_000_000_000L, btc.marketCap)
        assertEquals(1, btc.marketCapRank)
        assertEquals(25_000_000_000.0, btc.totalVolume)

        val request = mockServer.takeRequest()
        assertEquals("test-key", request.getHeader("x-cg-demo-api-key"))
        assert(request.path!!.contains("coins/markets"))
        assert(request.path!!.contains("vs_currency=usd"))
        assert(request.path!!.contains("ids=bitcoin"))
    }

    @Test
    fun `fetchSimplePrice should parse response correctly`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(SIMPLE_PRICE_RESPONSE)
        )

        val result = client.fetchSimplePrice(listOf("bitcoin", "ethereum"), "usd")

        assertEquals(2, result.size)
        val btc = result["bitcoin"]!!
        assertEquals("bitcoin", btc.coinId)
        assertEquals(50000.0, btc.price)
        assertEquals(25_000_000_000.0, btc.vol24h)
        assertEquals(2.5, btc.change24h)
        assertEquals(950_000_000_000.0, btc.marketCap)
        assertEquals(1710000000L, btc.lastUpdatedAt)
    }

    @Test
    fun `should throw on client error`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error": "bad request"}""")
        )

        assertThrows<RuntimeException> {
            client.fetchMarkets(listOf("bitcoin"), "usd", 250)
        }
    }

    @Test
    fun `should retry on server error`() {
        mockServer.enqueue(MockResponse().setResponseCode(500).setBody("error"))
        mockServer.enqueue(MockResponse().setResponseCode(500).setBody("error"))
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(MARKETS_RESPONSE)
        )

        val result = client.fetchMarkets(listOf("bitcoin"), "usd", 250)

        assertEquals(1, result.size)
        assertEquals(3, mockServer.requestCount)
    }

    companion object {
        val MARKETS_RESPONSE = """
            [
              {
                "id": "bitcoin",
                "symbol": "btc",
                "name": "Bitcoin",
                "current_price": 50000.0,
                "market_cap": 950000000000,
                "market_cap_rank": 1,
                "total_volume": 25000000000.0,
                "high_24h": 51000.0,
                "low_24h": 49000.0,
                "price_change_24h": 1000.0,
                "price_change_percentage_24h": 2.04,
                "price_change_percentage_1h_in_currency": 0.5,
                "price_change_percentage_7d_in_currency": 5.0,
                "price_change_percentage_14d_in_currency": -3.0,
                "price_change_percentage_30d_in_currency": 10.0,
                "circulating_supply": 19000000.0,
                "total_supply": 21000000.0,
                "ath": 69000.0,
                "ath_change_percentage": -27.54,
                "atl": 67.81,
                "atl_change_percentage": 73700.0,
                "last_updated": "2024-03-10T12:00:00.000Z"
              }
            ]
        """.trimIndent()

        val SIMPLE_PRICE_RESPONSE = """
            {
              "bitcoin": {
                "usd": 50000.0,
                "usd_24h_vol": 25000000000.0,
                "usd_24h_change": 2.5,
                "usd_market_cap": 950000000000.0,
                "last_updated_at": 1710000000
              },
              "ethereum": {
                "usd": 3000.0,
                "usd_24h_vol": 15000000000.0,
                "usd_24h_change": 1.8,
                "usd_market_cap": 360000000000.0,
                "last_updated_at": 1710000000
              }
            }
        """.trimIndent()
    }
}
