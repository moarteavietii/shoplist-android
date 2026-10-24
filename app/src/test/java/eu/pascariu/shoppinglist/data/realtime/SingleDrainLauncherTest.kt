package eu.pascariu.shoppinglist.data.realtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SingleDrainLauncherTest {

    @Test
    fun `second reachable flip while drain is running does not cancel it`() = runTest {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var drainRuns = 0
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val launcher = SingleDrainLauncher(scope) {
            drainRuns++
            started.complete(Unit)
            finish.await()
        }

        launcher.launchOrSkip()
        started.await()
        launcher.launchOrSkip()
        finish.complete(Unit)

        assertEquals(1, drainRuns)
        scope.coroutineContext.cancelChildren()
    }

    @Test
    fun `reachable flip after drain finished starts a new drain`() = runTest {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        var drainRuns = 0
        val launcher = SingleDrainLauncher(scope) {
            drainRuns++
        }

        launcher.launchOrSkip()
        launcher.launchOrSkip()

        assertEquals(2, drainRuns)
        scope.coroutineContext.cancelChildren()
    }

    @Test
    fun `cancel stops an in-flight drain`() = runTest {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val launcher = SingleDrainLauncher(scope) {
            started.complete(Unit)
            finish.await()
        }

        launcher.launchOrSkip()
        started.await()
        launcher.cancel()

        assertEquals(false, launcher.isActive())
        scope.coroutineContext.cancelChildren()
    }
}
