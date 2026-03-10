package com.stitch80.connect.coingecko

import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.connect.source.SourceTaskContext
import org.apache.kafka.connect.storage.OffsetStorageReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoinGeckoSourceTaskTest {

    @Test
    fun `should return correct version`() {
        val task = CoinGeckoSourceTask()
        assertEquals("1.0.0", task.version())
    }

    @Test
    fun `should initialize with valid config`() {
        val task = CoinGeckoSourceTask()
        val context = mockk<SourceTaskContext>()
        val offsetReader = mockk<OffsetStorageReader>()
        every { context.offsetStorageReader() } returns offsetReader

        task.initialize(context)

        val props = mapOf(
            CoinGeckoSourceConfig.API_KEY to "test-key",
            CoinGeckoSourceConfig.COIN_IDS to "bitcoin",
            CoinGeckoSourceConfig.POLL_INTERVAL_MS to "60000",
            CoinGeckoSourceConfig.TASK_ASSIGNED_COINS to "bitcoin",
        )

        task.start(props)
        task.stop()
    }
}
