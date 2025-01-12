package com.jetbrains.edu.aiHints.core.feedback

import com.jetbrains.educational.ml.hints.hint.TextHint
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class TextHintKSerializer : KSerializer<TextHint> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("TextHint", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): TextHint = TextHint(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: TextHint) = encoder.encodeString(value.text)
}