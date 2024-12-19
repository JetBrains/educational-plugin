package com.jetbrains.edu.learning.json.encrypt

interface Cipher {
  fun encrypt(bytes: ByteArray): ByteArray
  fun encrypt(str: String): String

  fun decrypt(bytes: ByteArray): ByteArray
  fun decrypt(str: String): String
}

fun Cipher(): Cipher = AES256Cipher()
