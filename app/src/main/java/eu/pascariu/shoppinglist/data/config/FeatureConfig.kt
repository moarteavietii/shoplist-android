package eu.pascariu.shoppinglist.data.config

import java.time.Duration

object FeatureConfig {
    val TEMPORARY_ITEM_TTL: Duration = Duration.ofDays(1)
    val EXPIRY_PRUNE_INTERVAL: Duration = Duration.ofMinutes(1)
}
