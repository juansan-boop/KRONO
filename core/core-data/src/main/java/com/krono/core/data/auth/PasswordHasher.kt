package com.krono.core.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Contraseña derivada: nunca se guarda el texto plano, solo el hash, la sal y las iteraciones. */
data class HashedPassword(
    val hash: String,
    val salt: String,
    val iterations: Int,
)

interface PasswordHasher {
    fun hash(password: String): HashedPassword
    fun verify(password: String, stored: HashedPassword): Boolean
}

/**
 * PBKDF2WithHmacSHA256 (javax.crypto, disponible desde API 26) con sal aleatoria
 * de 16 bytes por usuario. Las iteraciones se guardan con cada hash para poder
 * subirlas en el futuro sin invalidar contraseñas existentes.
 */
class Pbkdf2PasswordHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) : PasswordHasher {

    override fun hash(password: String): HashedPassword {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return HashedPassword(
            hash = encoder.encodeToString(derive(password, salt, iterations)),
            salt = encoder.encodeToString(salt),
            iterations = iterations,
        )
    }

    override fun verify(password: String, stored: HashedPassword): Boolean {
        val expected = decoder.decode(stored.hash)
        val actual = derive(password, decoder.decode(stored.salt), stored.iterations)
        return MessageDigest.isEqual(expected, actual) // comparación en tiempo constante
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val DEFAULT_ITERATIONS = 120_000
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_BYTES = 16
        private const val KEY_LENGTH_BITS = 256
        private val encoder = Base64.getEncoder()
        private val decoder = Base64.getDecoder()
    }
}
