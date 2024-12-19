package com.jetbrains.edu.learning.json.encrypt

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.databind.node.TextNode
import com.jetbrains.edu.learning.cipher.Cipher
import com.jetbrains.edu.learning.courseFormat.logger

private val LOG = logger<EncryptionModule>()

class EncryptionModule(private val cipher: Cipher = Cipher()) : Module() {
  override fun getModuleName(): String {
    return "edu-jackson-encryption-module"
  }

  override fun version(): Version {
    return Version(1, 0, 0, null, null as String?, null as String?)
  }

  override fun setupModule(setupContext: SetupContext) {
    setupContext.appendAnnotationIntrospector(EncryptAnnotationIntrospector(cipher))
  }
}

private class EncryptAnnotationIntrospector(private val cipher: Cipher) : NopAnnotationIntrospector() {
  override fun findDeserializer(am: Annotated): Any? {
    if (am.getAnnotation(Encrypt::class.java) != null) {
      return EncryptedJsonDeserializer(cipher)
    }
    return null
  }

  override fun findSerializer(am: Annotated): Any? {
    if (am.getAnnotation(Encrypt::class.java) != null) {
      return EncryptedJsonSerializer(cipher)
    }
    return null
  }
}

private class EncryptedJsonSerializer(private val cipher: Cipher) : JsonSerializer<Any>() {
  override fun serialize(value: Any, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
    if (value is String) {
      jsonGenerator.writeString(cipher.encrypt(value))
    }
    else {
      LOG.warning("@Encrypt annotation should not be used for a non-string field")
      serializerProvider.defaultSerializeValue(value, jsonGenerator)
    }
  }
}

private class EncryptedJsonDeserializer(private val cipher: Cipher) : JsonDeserializer<String>() {
  override fun deserialize(parser: JsonParser, context: DeserializationContext?): String {
    val node: TextNode = parser.readValueAsTree()
    return cipher.decrypt(node.asText())
  }
}
