package eu.pascariu.shoppinglist.data.connectivity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkAvailabilityTrackerTest {

    @Test
    fun `empty tracker reports no available network`() {
        val tracker = NetworkAvailabilityTracker<String>()
        assertFalse(tracker.hasAvailableNetwork())
    }

    @Test
    fun `available network makes tracker report availability`() {
        val tracker = NetworkAvailabilityTracker<String>()
        tracker.onNetworkAvailable("wifi")
        assertTrue(tracker.hasAvailableNetwork())
    }

    @Test
    fun `losing one of several networks keeps availability`() {
        val tracker = NetworkAvailabilityTracker<String>()
        tracker.onNetworkAvailable("wifi")
        tracker.onNetworkAvailable("cellular")
        tracker.onNetworkLost("wifi")
        assertTrue(tracker.hasAvailableNetwork())
    }

    @Test
    fun `losing the last network removes availability`() {
        val tracker = NetworkAvailabilityTracker<String>()
        tracker.onNetworkAvailable("wifi")
        tracker.onNetworkAvailable("cellular")
        tracker.onNetworkLost("wifi")
        tracker.onNetworkLost("cellular")
        assertFalse(tracker.hasAvailableNetwork())
    }

    @Test
    fun `losing an unknown network is a no-op`() {
        val tracker = NetworkAvailabilityTracker<String>()
        tracker.onNetworkLost("wifi")
        assertFalse(tracker.hasAvailableNetwork())
    }

    @Test
    fun `duplicate available events are idempotent`() {
        val tracker = NetworkAvailabilityTracker<String>()
        tracker.onNetworkAvailable("wifi")
        tracker.onNetworkAvailable("wifi")
        tracker.onNetworkLost("wifi")
        assertFalse(tracker.hasAvailableNetwork())
    }

    @Test
    fun `network can come back after being lost`() {
        val tracker = NetworkAvailabilityTracker<String>()
        tracker.onNetworkAvailable("wifi")
        tracker.onNetworkLost("wifi")
        tracker.onNetworkAvailable("wifi")
        assertTrue(tracker.hasAvailableNetwork())
    }
}
