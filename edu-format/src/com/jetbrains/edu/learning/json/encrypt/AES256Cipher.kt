package com.jetbrains.edu.learning.json.encrypt

import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

const val TEST_AES_KEY = "DFC929E375655998A34E56A21C98651C"

class AES256Cipher(private val secretKey: String = getAesKey()) : com.jetbrains.edu.learning.json.encrypt.Cipher {

  private fun cipher(opmode: Int, secretKey: String): Cipher {
    require(secretKey.length == 32) { "SecretKey length is not 32 chars" }
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")
    val iv = IvParameterSpec(secretKey.substring(0, 16).toByteArray(Charsets.UTF_8))
    cipher.init(opmode, secretKeySpec, iv)
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

  override fun encrypt(bytes: ByteArray): ByteArray = cipher(Cipher.ENCRYPT_MODE, secretKey).doFinal(bytes)

  override fun decrypt(bytes: ByteArray): ByteArray = cipher(Cipher.DECRYPT_MODE, secretKey).doFinal(bytes)

  companion object {
    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    private const val BUNDLE = "aes.aes"

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
