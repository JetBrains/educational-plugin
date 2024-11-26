package com.jetbrains.edu.ai.translation.feedback

import com.jetbrains.educational.core.format.enum.TranslationLanguage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class TranslationLanguageKSerializer : KSerializer<TranslationLanguage> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("TranslationLanguage", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: TranslationLanguage) {
    encoder.encodeString(value.label)
  }

  override fun deserialize(decoder: Decoder): TranslationLanguage {
    val label = decoder.decodeString()
    return TranslationLanguage.findByLabel(label) ?: error("Error: TranslationLanguage with label '$label' not found")
  }
}