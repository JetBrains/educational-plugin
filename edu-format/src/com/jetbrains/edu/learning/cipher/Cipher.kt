package com.jetbrains.edu.learning.cipher

interface Cipher {
  fun encrypt(bytes: ByteArray): ByteArray
  fun encrypt(str: String): String

  fun decrypt(bytes: ByteArray): ByteArray
  fun decrypt(str: String): String
}

fun Cipher(): Cipher = AES256Cipher.create()

/**
 * Returns a cipher which is supposed to be used in tests
 */
@Suppress("FunctionName")
fun TestCipher(): Cipher = AES256Cipher.TEST_CIPHER

/**
 * Returns a cipher which applies identity transformation for given [String] or [ByteArray],
 * i.e., it doesn't transform a given object anyhow and return it unmodified
 */
@Suppress("FunctionName")
fun NoOpCipher(): Cipher = NoOpCipher
