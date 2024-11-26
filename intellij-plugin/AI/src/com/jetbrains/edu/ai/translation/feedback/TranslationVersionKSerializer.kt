package com.jetbrains.edu.ai.translation.feedback

import com.jetbrains.educational.translation.format.domain.TranslationVersion
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class TranslationVersionKSerializer : KSerializer<TranslationVersion> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("TranslationVersion", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: TranslationVersion) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): TranslationVersion {
    val version = decoder.decodeInt()
    return TranslationVersion(version)
  }
}