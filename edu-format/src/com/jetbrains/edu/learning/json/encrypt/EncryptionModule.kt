package com.jetbrains.edu.learning.json.encrypt

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.databind.node.TextNode
import com.jetbrains.edu.learning.courseFormat.logger
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val BUNDLE = "aes.aes"
private val LOG = logger<EncryptionModule>()
const val TEST_AES_KEY = "DFC929E375655998A34E56A21C98651C"

class EncryptionModule(private val aesKey: String?) : Module() {
  override fun getModuleName(): String {
    return "edu-jackson-encryption-module"
  }

  override fun version(): Version {
    return Version(1, 0, 0, null, null as String?, null as String?)
  }

  override fun setupModule(setupContext: SetupContext) {
    if (aesKey != null) {
      setupContext.appendAnnotationIntrospector(EncryptAnnotationIntrospector(aesKey))
    }
  }
}

private class EncryptAnnotationIntrospector(private val aesKey: String) : NopAnnotationIntrospector() {
  override fun findDeserializer(am: Annotated): Any? {
    if (am.getAnnotation(Encrypt::class.java) != null) {
      return EncryptedJsonDeserializer(aesKey)
    }
    return null
  }

  override fun findSerializer(am: Annotated): Any? {
    if (am.getAnnotation(Encrypt::class.java) != null) {
      return EncryptedJsonSerializer(aesKey)
    }
    return null
  }
}

private class EncryptedJsonSerializer(private val aesKey: String) : JsonSerializer<Any>() {
  override fun serialize(value: Any, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
    if (value is String) {
      jsonGenerator.writeString(AES256.encrypt(value, aesKey))
    }
    else {
      LOG.warning("@Encrypt annotation should not be used for a non-string field")
      serializerProvider.defaultSerializeValue(value, jsonGenerator)
    }
  }
}

private class EncryptedJsonDeserializer(private val aesKey: String) : JsonDeserializer<String>() {
  override fun deserialize(parser: JsonParser, context: DeserializationContext?): String {
    val node: TextNode = parser.readValueAsTree()
    return AES256.decrypt(node.asText(), aesKey)
  }
}

object AES256 {
  private val encoder = Base64.getEncoder()
  private val decoder = Base64.getDecoder()

  private fun cipher(opmode: Int, secretKey: String): Cipher {
    require(secretKey.length == 32) { "SecretKey length is not 32 chars" }
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")
    val iv = IvParameterSpec(secretKey.substring(0, 16).toByteArray(Charsets.UTF_8))
    cipher.init(opmode, secretKeySpec, iv)
    return cipher
  }

  fun encrypt(str: String, secretKey: String): String {
    val encrypted = cipher(Cipher.ENCRYPT_MODE, secretKey).doFinal(str.toByteArray(Charsets.UTF_8))
    return String(encoder.encode(encrypted), Charsets.UTF_8)
  }

  fun decrypt(str: String, secretKey: String): String {
    val byteStr = decoder.decode(str.toByteArray(Charsets.UTF_8))
    return String(cipher(Cipher.DECRYPT_MODE, secretKey).doFinal(byteStr), Charsets.UTF_8)
  }

  fun encryptBinary(bytes: ByteArray, secretKey: String): ByteArray = cipher(Cipher.ENCRYPT_MODE, secretKey).doFinal(bytes)

  fun decryptBinary(bytes: ByteArray, secretKey: String): ByteArray = cipher(Cipher.DECRYPT_MODE, secretKey).doFinal(bytes)
}



fun getAesKey(): String {
  return try {
    val resourceBundle = ResourceBundle.getBundle(BUNDLE)
    resourceBundle.getString("aesKey")
  }
  catch (e: Exception) {
    TEST_AES_KEY
  }
}