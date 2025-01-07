package com.jetbrains.edu.aiHints.core.feedback

import com.jetbrains.educational.ml.hints.hint.CodeHint
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class CodeHintSerializer : KSerializer<CodeHint> {
  override fun deserialize(decoder: Decoder): CodeHint = CodeHint(decoder.decodeString())

  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("CodeHint", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: CodeHint) = encoder.encodeString(value.code)
}