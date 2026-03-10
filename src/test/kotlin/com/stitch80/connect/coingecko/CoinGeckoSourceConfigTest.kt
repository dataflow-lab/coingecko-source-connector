package com.stitch80.connect.coingecko

import org.apache.kafka.common.config.ConfigException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CoinGeckoSourceConfigTest {

    private fun baseProps(): MutableMap<String, String> = mutableMapOf(
        CoinGeckoSourceConfig.API_KEY to "test-api-key",
    )

    @Test
    fun `should parse default configuration`() {
        val config = CoinGeckoSourceConfig(baseProps())

        assertEquals(listOf("bitcoin", "ethereum"), config.coinIds())
        assertEquals("usd", config.vsCurrency())
        assertEquals("raw.coingecko", config.topicPrefix())
        assertEquals(60_000L, config.pollIntervalMs())
        assertEquals(setOf("markets", "simple_price"), config.enabledEndpoints())
        assertEquals(250, config.marketsPerPage())
        assertEquals(25, config.rateLimitCallsPerMinute())
        assertEquals(30_000L, config.requestTimeoutMs())
    }

    @Test
    fun `should parse custom coin IDs`() {
        val props = baseProps().apply {
            put(CoinGeckoSourceConfig.COIN_IDS, "bitcoin,solana,cardano")
        }
        val config = CoinGeckoSourceConfig(props)

        assertEquals(listOf("bitcoin", "solana", "cardano"), config.coinIds())
    }

    @Test
    fun `should reject empty coin IDs`() {
        val props = baseProps().apply {
            put(CoinGeckoSourceConfig.COIN_IDS, "")
        }
        assertThrows<ConfigException> { CoinGeckoSourceConfig(props) }
    }

    @Test
    fun `should reject invalid endpoints`() {
        val props = baseProps().apply {
            put(CoinGeckoSourceConfig.ENDPOINTS, "markets,invalid_endpoint")
        }
        assertThrows<ConfigException> { CoinGeckoSourceConfig(props) }
    }

    @Test
    fun `should reject poll interval below minimum`() {
        val props = baseProps().apply {
            put(CoinGeckoSourceConfig.POLL_INTERVAL_MS, "1000")
        }
        assertThrows<ConfigException> { CoinGeckoSourceConfig(props) }
    }

    @Test
    fun `should return correct API base URL and header for demo tier`() {
        val config = CoinGeckoSourceConfig(baseProps())

        assertEquals("https://api.coingecko.com/api/v3/", config.baseUrl())
        assertEquals("x-cg-demo-api-key", config.apiKeyHeader())
    }

    @Test
    fun `should use task assigned coins when set`() {
        val props = baseProps().apply {
            put(CoinGeckoSourceConfig.TASK_ASSIGNED_COINS, "solana,cardano")
        }
        val config = CoinGeckoSourceConfig(props)

        assertEquals(listOf("solana", "cardano"), config.taskAssignedCoins())
    }

    @Test
    fun `should fall back to coin IDs when task assigned coins not set`() {
        val config = CoinGeckoSourceConfig(baseProps())

        assertEquals(listOf("bitcoin", "ethereum"), config.taskAssignedCoins())
    }

    @Test
    fun `should require API key`() {
        assertThrows<ConfigException> { CoinGeckoSourceConfig(emptyMap()) }
    }
}
