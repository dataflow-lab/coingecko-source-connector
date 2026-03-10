package com.stitch80.connect.coingecko.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CoinMarketData(
    @JsonProperty("id") val id: String,
    @JsonProperty("symbol") val symbol: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("current_price") val currentPrice: Double,
    @JsonProperty("market_cap") val marketCap: Long,
    @JsonProperty("market_cap_rank") val marketCapRank: Int?,
    @JsonProperty("total_volume") val totalVolume: Double,
    @JsonProperty("high_24h") val high24h: Double?,
    @JsonProperty("low_24h") val low24h: Double?,
    @JsonProperty("price_change_24h") val priceChange24h: Double?,
    @JsonProperty("price_change_percentage_24h") val priceChangePercentage24h: Double?,
    @JsonProperty("price_change_percentage_1h_in_currency") val priceChangePercentage1h: Double?,
    @JsonProperty("price_change_percentage_7d_in_currency") val priceChangePercentage7d: Double?,
    @JsonProperty("price_change_percentage_14d_in_currency") val priceChangePercentage14d: Double?,
    @JsonProperty("price_change_percentage_30d_in_currency") val priceChangePercentage30d: Double?,
    @JsonProperty("circulating_supply") val circulatingSupply: Double?,
    @JsonProperty("total_supply") val totalSupply: Double?,
    @JsonProperty("ath") val ath: Double?,
    @JsonProperty("ath_change_percentage") val athChangePercentage: Double?,
    @JsonProperty("atl") val atl: Double?,
    @JsonProperty("atl_change_percentage") val atlChangePercentage: Double?,
    @JsonProperty("last_updated") val lastUpdated: String,
)
