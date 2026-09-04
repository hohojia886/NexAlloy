package io.github.nexalloy

import io.github.nexalloy.compat.LSPosedCompat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LSPosedCompatTest {

    @Test
    fun testIsApi102AvailableDefaultFalse() {
        assertFalse(LSPosedCompat.isApi102Available())
    }

    @Test
    fun testDummyMethodReflect() {
        val method = String::class.java.getMethod("toString")
        assertNotNull(method)
    }
}
