package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.FORMAT_VERSION
import com.jetbrains.edu.learning.serialization.converter.json.*
import com.jetbrains.edu.learning.stepik.StepikWrappers
import java.lang.reflect.Type

class StepikStepOptionsAdapter(val language: String?) : JsonDeserializer<StepikWrappers.StepOptions> {

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): StepikWrappers.StepOptions {
    var stepOptionsJson = json.asJsonObject
    val versionJson = stepOptionsJson.getAsJsonPrimitive(FORMAT_VERSION)
    var version = 1
    if (versionJson != null) {
      version = versionJson.asInt
    }
    while (version < JSON_FORMAT_VERSION) {
      when (version) {
        1 -> stepOptionsJson = convertToSecondVersion(stepOptionsJson)
        2 -> stepOptionsJson = convertToThirdVersion(stepOptionsJson)
        3 -> stepOptionsJson = convertToFourthVersion(stepOptionsJson)
        4 -> stepOptionsJson = convertToFifthVersion(stepOptionsJson)
        5 -> stepOptionsJson = convertToSixthVersion(stepOptionsJson)
        6 -> stepOptionsJson = convertToSeventhVersion(stepOptionsJson)
      }
      version++
    }
    val stepOptions = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
      .fromJson(stepOptionsJson, StepikWrappers.StepOptions::class.java)
    stepOptions.formatVersion = JSON_FORMAT_VERSION
    return stepOptions
  }

  private fun convertToSecondVersion(stepOptionsJson: JsonObject): JsonObject {
    return ToSecondVersionJsonStepOptionsConverter().convert(stepOptionsJson)
  }

  private fun convertToThirdVersion(stepOptionsJson: JsonObject): JsonObject {
    return ToThirdVersionJsonStepOptionsConverter().convert(stepOptionsJson)
  }

  private fun convertToFourthVersion(stepOptionsJson: JsonObject): JsonObject {
    return ToFourthVersionJsonStepOptionsConverter().convert(stepOptionsJson)
  }

  private fun convertToFifthVersion(stepOptionsJson: JsonObject): JsonObject {
    return ToFifthVersionJsonStepOptionsConverter().convert(stepOptionsJson)
  }

  private fun convertToSixthVersion(stepOptionsJson: JsonObject): JsonObject {
    return ToSixthVersionJsonStepOptionConverter().convert(stepOptionsJson)
  }

  private fun convertToSeventhVersion(stepOptionsJson: JsonObject): JsonObject {
    return ToSeventhVersionJsonStepOptionConverter(language).convert(stepOptionsJson)
  }
}
