package com.stitch80.connect.coingecko.converter

import com.stitch80.connect.coingecko.model.CoinMarketData
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct

object MarketDataConverter {

    val SCHEMA: Schema = SchemaBuilder.struct()
        .name("com.stitch80.connect.coingecko.avro.CoinMarketEvent")
        .field("coinId", Schema.STRING_SCHEMA)
        .field("symbol", Schema.STRING_SCHEMA)
        .field("name", Schema.STRING_SCHEMA)
        .field("currentPrice", Schema.FLOAT64_SCHEMA)
        .field("marketCap", Schema.INT64_SCHEMA)
        .field("marketCapRank", Schema.OPTIONAL_INT32_SCHEMA)
        .field("totalVolume", Schema.FLOAT64_SCHEMA)
        .field("high24h", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("low24h", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("priceChange24h", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("priceChangePercentage24h", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("priceChangePercentage1h", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("priceChangePercentage7d", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("priceChangePercentage14d", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("priceChangePercentage30d", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("circulatingSupply", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("totalSupply", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("ath", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("athChangePercentage", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("atl", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("atlChangePercentage", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("lastUpdated", Schema.STRING_SCHEMA)
        .field("ingestionTime", Schema.INT64_SCHEMA)
        .build()

    fun toStruct(data: CoinMarketData, ingestionTime: Long): Struct =
        Struct(SCHEMA)
            .put("coinId", data.id)
            .put("symbol", data.symbol)
            .put("name", data.name)
            .put("currentPrice", data.currentPrice)
            .put("marketCap", data.marketCap)
            .put("marketCapRank", data.marketCapRank)
            .put("totalVolume", data.totalVolume)
            .put("high24h", data.high24h)
            .put("low24h", data.low24h)
            .put("priceChange24h", data.priceChange24h)
            .put("priceChangePercentage24h", data.priceChangePercentage24h)
            .put("priceChangePercentage1h", data.priceChangePercentage1h)
            .put("priceChangePercentage7d", data.priceChangePercentage7d)
            .put("priceChangePercentage14d", data.priceChangePercentage14d)
            .put("priceChangePercentage30d", data.priceChangePercentage30d)
            .put("circulatingSupply", data.circulatingSupply)
            .put("totalSupply", data.totalSupply)
            .put("ath", data.ath)
            .put("athChangePercentage", data.athChangePercentage)
            .put("atl", data.atl)
            .put("atlChangePercentage", data.atlChangePercentage)
            .put("lastUpdated", data.lastUpdated)
            .put("ingestionTime", ingestionTime)
}
