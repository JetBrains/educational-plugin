package com.jetbrains.edu.learning.cipher

internal object NoOpCipher : Cipher {
  override fun encrypt(bytes: ByteArray): ByteArray = bytes
  override fun encrypt(str: String): String = str

  override fun decrypt(bytes: ByteArray): ByteArray = bytes
  override fun decrypt(str: String): String = str
}
