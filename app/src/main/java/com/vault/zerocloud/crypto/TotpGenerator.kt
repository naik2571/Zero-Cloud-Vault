package com.vault.zerocloud.crypto

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 6238 Time-based One-Time Password (TOTP) Offline Engine.
 * Generates 6-digit 2FA security codes completely air-gapped without network access.
 */
object TotpGenerator {

    private const val DEFAULT_TIME_STEP_SECONDS = 30L

    /**
     * Generates a 6-digit TOTP code for the given Base32 secret key.
     */
    fun generateTotp(secretBase32: String, timeStepSeconds: Long = DEFAULT_TIME_STEP_SECONDS): String {
        return try {
            val cleanKey = secretBase32.replace(" ", "").replace("-", "").uppercase()
            val keyBytes = decodeBase32(cleanKey)
            val currentCounter = (System.currentTimeMillis() / 1000L) / timeStepSeconds

            val buffer = ByteBuffer.allocate(8).putLong(currentCounter)
            val counterBytes = buffer.array()

            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
            val hash = mac.doFinal(counterBytes)

            val offset = hash[hash.size - 1].toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % 1000000
            String.format("%06d", otp)
        } catch (e: Exception) {
            "------"
        }
    }

    /**
     * Returns the remaining seconds in the current 30s window.
     */
    fun getRemainingSeconds(timeStepSeconds: Long = DEFAULT_TIME_STEP_SECONDS): Int {
        val currentSeconds = (System.currentTimeMillis() / 1000L) % timeStepSeconds
        return (timeStepSeconds - currentSeconds).toInt()
    }

    private fun decodeBase32(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val out = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (c in input) {
            if (c == '=') break
            val value = alphabet.indexOf(c)
            if (value < 0) continue

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                out.add((buffer shr (bitsLeft - 8) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }
}
