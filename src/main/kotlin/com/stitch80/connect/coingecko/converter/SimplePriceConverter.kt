package com.stitch80.connect.coingecko.converter

import com.stitch80.connect.coingecko.model.CoinSimplePrice
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct

object SimplePriceConverter {

    val SCHEMA: Schema = SchemaBuilder.struct()
        .name("com.stitch80.connect.coingecko.avro.CoinSimplePriceEvent")
        .field("coinId", Schema.STRING_SCHEMA)
        .field("price", Schema.FLOAT64_SCHEMA)
        .field("vol24h", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("change24h", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("marketCap", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("lastUpdatedAt", Schema.INT64_SCHEMA)
        .field("ingestionTime", Schema.INT64_SCHEMA)
        .build()

    fun toStruct(data: CoinSimplePrice, ingestionTime: Long): Struct =
        Struct(SCHEMA)
            .put("coinId", data.coinId)
            .put("price", data.price ?: 0.0)
            .put("vol24h", data.vol24h)
            .put("change24h", data.change24h)
            .put("marketCap", data.marketCap)
            .put("lastUpdatedAt", data.lastUpdatedAt ?: 0L)
            .put("ingestionTime", ingestionTime)
}
