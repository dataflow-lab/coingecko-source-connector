package com.stitch80.connect.coingecko

import mu.KotlinLogging
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector

private val logger = KotlinLogging.logger {}

class CoinGeckoSourceConnector : SourceConnector() {

    private lateinit var configProps: Map<String, String>
    private lateinit var config: CoinGeckoSourceConfig

    override fun version(): String = VERSION

    override fun start(props: Map<String, String>) {
        configProps = props
        config = CoinGeckoSourceConfig(props)
        logger.info {
            "Starting CoinGecko source connector: coins=${config.coinIds()}, " +
                "endpoints=${config.enabledEndpoints()}, pollInterval=${config.pollIntervalMs()}ms"
        }
    }

    override fun taskClass(): Class<out Task> = CoinGeckoSourceTask::class.java

    override fun taskConfigs(maxTasks: Int): List<Map<String, String>> {
        val coinIds = config.coinIds()
        val chunked = coinIds.chunked((coinIds.size + maxTasks - 1) / maxTasks)

        logger.info { "Distributing ${coinIds.size} coins across ${chunked.size} task(s)" }

        return chunked.map { chunk ->
            configProps + mapOf(CoinGeckoSourceConfig.TASK_ASSIGNED_COINS to chunk.joinToString(","))
        }
    }

    override fun stop() {
        logger.info { "CoinGecko source connector stopped" }
    }

    override fun config(): ConfigDef = CoinGeckoSourceConfig.CONFIG_DEF

    companion object {
        const val VERSION = "1.0.0"
    }
}
