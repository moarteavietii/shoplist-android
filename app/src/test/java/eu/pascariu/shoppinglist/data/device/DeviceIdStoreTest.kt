package eu.pascariu.shoppinglist.data.device

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdStoreTest {

    @Test
    fun `accepts uuid style device id`() {
        assertTrue(isValidDeviceId("3f2a9c1e-8b4d-4e7a-9c2f-1d6b5a0e7f83"))
    }

    @Test
    fun `accepts alphanumeric underscore and dash`() {
        assertTrue(isValidDeviceId("Abc_123-xyz"))
    }

    @Test
    fun `rejects empty value`() {
        assertFalse(isValidDeviceId(""))
    }

    @Test
    fun `rejects value longer than 64 chars`() {
        assertFalse(isValidDeviceId("a".repeat(65)))
        assertTrue(isValidDeviceId("a".repeat(64)))
    }

    @Test
    fun `rejects illegal characters`() {
        assertFalse(isValidDeviceId("id with spaces"))
        assertFalse(isValidDeviceId("id\nnewline"))
        assertFalse(isValidDeviceId("id/slash"))
        assertFalse(isValidDeviceId("id:colon"))
        assertFalse(isValidDeviceId("idéaccent"))
    }
}
