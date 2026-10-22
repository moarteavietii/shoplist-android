package eu.pascariu.shoppinglist.data.undo

import java.time.Duration

object UndoConfig {
    const val MAX_ENTRIES = 50
    val TTL: Duration = Duration.ofMinutes(30)
    val PRUNE_INTERVAL: Duration = Duration.ofSeconds(60)
    val INFLIGHT_WAIT: Duration = Duration.ofSeconds(2)
}
