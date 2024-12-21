package com.jetbrains.edu.learning.cipher

import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class AES256Cipher private constructor(
  private val keySpec: SecretKeySpec,
  private val ivSpec: IvParameterSpec
) : com.jetbrains.edu.learning.cipher.Cipher {

  private fun cipher(opmode: Int): Cipher {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(opmode, keySpec, ivSpec)
    return cipher
  }

  override fun encrypt(str: String): String {
    val encrypted = encrypt(str.toByteArray(Charsets.UTF_8))
    return String(encoder.encode(encrypted), Charsets.UTF_8)
  }

  override fun decrypt(str: String): String {
    val byteStr = decoder.decode(str.toByteArray(Charsets.UTF_8))
    return String(decrypt(byteStr), Charsets.UTF_8)
  }

  override fun encrypt(bytes: ByteArray): ByteArray = cipher(Cipher.ENCRYPT_MODE).doFinal(bytes)

  override fun decrypt(bytes: ByteArray): ByteArray = cipher(Cipher.DECRYPT_MODE).doFinal(bytes)

  companion object {
    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    private const val BUNDLE = "aes.aes"

    private const val TEST_AES_KEY = "DFC929E375655998A34E56A21C98651C"

    val TEST_CIPHER: AES256Cipher = create(TEST_AES_KEY)

    fun create(secretKey: String = getAesKey()): AES256Cipher {
      require(secretKey.length == 32) { "SecretKey length is not 32 chars" }

      val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")
      val iv = IvParameterSpec(secretKey.substring(0, 16).toByteArray(Charsets.UTF_8))

      return AES256Cipher(secretKeySpec, iv)
    }

    private fun getAesKey(): String {
      return try {
        val resourceBundle = ResourceBundle.getBundle(BUNDLE)
        resourceBundle.getString("aesKey")
      }
      catch (e: Exception) {
        TEST_AES_KEY
      }
    }
  }
}
