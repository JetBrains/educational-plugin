package com.jetbrains.edu.aiHints.core.feedback

import com.jetbrains.educational.ml.hints.hint.CodeHint
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class CodeHintKSerializer : KSerializer<CodeHint> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("CodeHint", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): CodeHint = CodeHint(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: CodeHint) = encoder.encodeString(value.code)
}