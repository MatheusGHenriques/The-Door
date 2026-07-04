package io.github.matheusghenriques.thedoor.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinManagerTest {

    @Test
    fun hashPin_returnsNonEmptyString() {
        val hash = PinManager.hashPin("123456")
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
    }

    @Test
    fun verifyPin_correctPin_returnsTrue() {
        val hash = PinManager.hashPin("654321")
        assertTrue(PinManager.verifyPin("654321", hash))
    }

    @Test
    fun verifyPin_wrongPin_returnsFalse() {
        val hash = PinManager.hashPin("654321")
        assertFalse(PinManager.verifyPin("123456", hash))
    }

    @Test
    fun verifyPin_differentHashes_doesNotMatch() {
        val hash1 = PinManager.hashPin("111111")
        val hash2 = PinManager.hashPin("222222")
        assertFalse(PinManager.verifyPin("111111", hash2))
        assertFalse(PinManager.verifyPin("222222", hash1))
    }
}
