package io.github.matheusghenriques.thedoor.data

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom

object PinManager {
    private const val HASH_LENGTH = 32
    private const val T_COST = 3
    private const val M_COST = 65536
    private const val PARALLELISM = 1

    private val argon2 by lazy { Argon2Kt() }

    fun hashPin(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = pin.toByteArray(),
            salt = salt,
            tCostInIterations = T_COST,
            mCostInKibibyte = M_COST,
            parallelism = PARALLELISM,
            hashLengthInBytes = HASH_LENGTH
        )
        return result.encodedOutputAsString()
    }

    fun verifyPin(pin: String, encodedHash: String): Boolean {
        return try {
            argon2.verify(
                mode = Argon2Mode.ARGON2_ID, encoded = encodedHash, password = pin.toByteArray()
            )
        } catch (_: Exception) {
            false
        }
    }
}
