package com.stitch80.connect.coingecko.api

import mu.KotlinLogging
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class RateLimiter(private val callsPerMinute: Int) : Closeable {

    private val semaphore = Semaphore(callsPerMinute)
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "rate-limiter-replenish").apply { isDaemon = true }
    }

    init {
        scheduler.scheduleAtFixedRate(::replenish, 60, 60, TimeUnit.SECONDS)
        logger.info { "Rate limiter initialized: $callsPerMinute calls/minute" }
    }

    fun acquire() {
        semaphore.acquire()
    }

    private fun replenish() {
        val permitsToRelease = callsPerMinute - semaphore.availablePermits()
        if (permitsToRelease > 0) {
            semaphore.release(permitsToRelease)
            logger.debug { "Replenished $permitsToRelease rate limit permits" }
        }
    }

    override fun close() {
        scheduler.shutdown()
        scheduler.awaitTermination(5, TimeUnit.SECONDS)
    }
}
