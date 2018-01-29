package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.SerializationUtils.PLACEHOLDERS
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
    when (version) {
      1 -> {
        stepOptionsJson = convertToSecondVersion(stepOptionsJson)
        stepOptionsJson = convertToThirdVersion(stepOptionsJson)
        stepOptionsJson = convertToFourthVersion(stepOptionsJson)
      }
      2 -> {
        stepOptionsJson = convertToThirdVersion(stepOptionsJson)
        stepOptionsJson = convertToFourthVersion(stepOptionsJson)
      }
      3 -> stepOptionsJson = convertToFourthVersion(stepOptionsJson)
    }// uncomment for future versions
    //case 4:
    //  stepOptionsJson = convertToFourthVersion(stepOptionsJson);
    convertSubtaskInfosToMap(stepOptionsJson)
    val stepOptions = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
      .fromJson(stepOptionsJson, StepikWrappers.StepOptions::class.java)
    stepOptions.formatVersion = StepikConnector.CURRENT_VERSION
    return stepOptions
  }

  private fun convertToSecondVersion(stepOptionsJson: JsonObject): JsonObject {
    return ToSecondVersionJsonStepOptionsConverter().convert(stepOptionsJson)
  }

  private fun convertToFourthVersion(stepOptionsJson: JsonObject): JsonObject {
    return ToFourthVersionJsonStepOptionsConverter().convert(stepOptionsJson)
  }

  private fun convertToThirdVersion(stepOptionsJson: JsonObject): JsonObject {
    return ToThirdVersionJsonStepOptionsConverter().convert(stepOptionsJson)
  }

  private fun convertSubtaskInfosToMap(stepOptionsJson: JsonObject): JsonObject {
    val files = stepOptionsJson.getAsJsonArray(FILES)
    if (files != null) {
      for (taskFileElement in files) {
        val taskFileObject = taskFileElement.asJsonObject
        val placeholders = taskFileObject.getAsJsonArray(PLACEHOLDERS)
        for (placeholder in placeholders) {
          val placeholderObject = placeholder.asJsonObject
          removeIndexFromSubtaskInfos(placeholderObject)
        }
      }
    }
    return stepOptionsJson
  }
}