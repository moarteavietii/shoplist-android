package eu.pascariu.shoppinglist.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class MutationKindConverterTest {

    private val converter = MutationKindConverter()

    @Test
    fun `round-trips every mutation kind`() {
        MutationKind.entries.forEach { kind ->
            assertEquals(kind, converter.toKind(converter.fromKind(kind)))
        }
    }

    @Test
    fun `stored form is the enum name`() {
        MutationKind.entries.forEach { kind ->
            assertEquals(kind.name, converter.fromKind(kind))
        }
    }
}
