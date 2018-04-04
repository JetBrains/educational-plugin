package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.converter.json.ToFifthVersionJsonStepOptionsConverter
import com.jetbrains.edu.learning.serialization.converter.json.ToFourthVersionJsonStepOptionsConverter
import com.jetbrains.edu.learning.serialization.converter.json.ToSecondVersionJsonStepOptionsConverter
import com.jetbrains.edu.learning.serialization.converter.json.ToThirdVersionJsonStepOptionsConverter
import com.jetbrains.edu.learning.stepik.StepikConnector
import com.jetbrains.edu.learning.stepik.StepikWrappers
import java.lang.reflect.Type

class StepikStepOptionsAdapter : JsonDeserializer<StepikWrappers.StepOptions> {
  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): StepikWrappers.StepOptions {
    var stepOptionsJson = json.asJsonObject
    val versionJson = stepOptionsJson.getAsJsonPrimitive(FORMAT_VERSION)
    var version = 1
    if (versionJson != null) {
      version = versionJson.asInt
    }
    loop@while (true) {
      stepOptionsJson = when (version) {
        1 -> convertToSecondVersion(stepOptionsJson)
        2 -> convertToThirdVersion(stepOptionsJson)
        3 -> convertToFourthVersion(stepOptionsJson)
        4 -> convertToFifthVersion(stepOptionsJson)
        else -> break@loop
      }
      version++
    }
    val stepOptions = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
      .fromJson(stepOptionsJson, StepikWrappers.StepOptions::class.java)
    stepOptions.formatVersion = StepikConnector.CURRENT_VERSION
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
}