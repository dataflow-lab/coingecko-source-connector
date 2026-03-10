package com.stitch80.connect.coingecko

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoinGeckoSourceConnectorTest {

    private fun baseProps(): Map<String, String> = mapOf(
        CoinGeckoSourceConfig.API_KEY to "test-api-key",
        CoinGeckoSourceConfig.COIN_IDS to "bitcoin,ethereum,solana,cardano,polkadot",
    )

    @Test
    fun `should return correct version`() {
        val connector = CoinGeckoSourceConnector()
        assertEquals("1.0.0", connector.version())
    }

    @Test
    fun `should return correct task class`() {
        val connector = CoinGeckoSourceConnector()
        assertEquals(CoinGeckoSourceTask::class.java, connector.taskClass())
    }

    @Test
    fun `should distribute coins across single task`() {
        val connector = CoinGeckoSourceConnector()
        connector.start(baseProps())

        val configs = connector.taskConfigs(1)

        assertEquals(1, configs.size)
        assertEquals("bitcoin,ethereum,solana,cardano,polkadot", configs[0][CoinGeckoSourceConfig.TASK_ASSIGNED_COINS])
    }

    @Test
    fun `should distribute coins across multiple tasks`() {
        val connector = CoinGeckoSourceConnector()
        connector.start(baseProps())

        val configs = connector.taskConfigs(3)

        assertEquals(3, configs.size)
        val allCoins = configs.flatMap {
            it[CoinGeckoSourceConfig.TASK_ASSIGNED_COINS]!!.split(",")
        }
        assertEquals(5, allCoins.size)
        assertTrue(allCoins.containsAll(listOf("bitcoin", "ethereum", "solana", "cardano", "polkadot")))
    }

    @Test
    fun `should propagate config to task configs`() {
        val connector = CoinGeckoSourceConnector()
        connector.start(baseProps())

        val configs = connector.taskConfigs(1)

        assertEquals("test-api-key", configs[0][CoinGeckoSourceConfig.API_KEY])
    }

    @Test
    fun `should return valid config def`() {
        val connector = CoinGeckoSourceConnector()
        val configDef = connector.config()

        assertTrue(configDef.names().contains(CoinGeckoSourceConfig.API_KEY))
        assertTrue(configDef.names().contains(CoinGeckoSourceConfig.COIN_IDS))
        assertTrue(configDef.names().contains(CoinGeckoSourceConfig.TOPIC_PREFIX))
    }
}
