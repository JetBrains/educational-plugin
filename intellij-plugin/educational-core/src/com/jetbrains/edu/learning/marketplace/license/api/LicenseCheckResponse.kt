package com.jetbrains.edu.learning.marketplace.license.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

// Format of license response from a learning center
@JsonDeserialize(using = LicenseCheckResponseDeserializer::class)
sealed interface LicenseCheckResponse {
  object Ok : LicenseCheckResponse

  data class Error(val trackType: TrackType) : LicenseCheckResponse {
    enum class TrackType { AWS }
  }
}

class LicenseCheckResponseDeserializer : JsonDeserializer<LicenseCheckResponse>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LicenseCheckResponse {
    val node = p.readValueAsTree<JsonNode>()
    return if (node.has("trackType")) {
      val nodeValue = node.get("trackType").asText()
      val trackType = LicenseCheckResponse.Error.TrackType.valueOf(nodeValue)
      LicenseCheckResponse.Error(trackType)
    }
    else {
      LicenseCheckResponse.Ok
    }
  }
}