package com.stitch80.connect.coingecko.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CoinSimplePrice(
    val coinId: String,
    @JsonProperty("usd") val price: Double?,
    @JsonProperty("usd_24h_vol") val vol24h: Double?,
    @JsonProperty("usd_24h_change") val change24h: Double?,
    @JsonProperty("usd_market_cap") val marketCap: Double?,
    @JsonProperty("last_updated_at") val lastUpdatedAt: Long?,
)
