package com.jetbrains.edu.learning.stepik.serialization

import com.google.common.annotations.VisibleForTesting
import com.google.gson.*
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.FORMAT_VERSION
import com.jetbrains.edu.learning.serialization.converter.json.*
import com.jetbrains.edu.learning.stepik.StepikWrappers
import java.lang.reflect.Type

class StepikStepOptionsAdapter(val language: String?) : JsonDeserializer<StepikWrappers.StepOptions> {

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): StepikWrappers.StepOptions {
    val stepOptionsJson = migrate(json.asJsonObject, JSON_FORMAT_VERSION, language)
    return GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
      .fromJson(stepOptionsJson, StepikWrappers.StepOptions::class.java)
  }

  companion object {
    @VisibleForTesting
    @JvmOverloads
    @JvmStatic
    fun migrate(stepOptionsJson: JsonObject, maxVersion: Int, language: String? = null): JsonObject {
      var convertedStepOptions = stepOptionsJson
      val versionJson = stepOptionsJson.getAsJsonPrimitive(FORMAT_VERSION)
      var version = 1
      if (versionJson != null) {
        version = versionJson.asInt
      }
      while (version < maxVersion) {
        val converter = when (version) {
          1 -> ToSecondVersionJsonStepOptionsConverter()
          2 -> ToThirdVersionJsonStepOptionsConverter()
          3 -> ToFourthVersionJsonStepOptionsConverter()
          4 -> ToFifthVersionJsonStepOptionsConverter()
          5 -> ToSixthVersionJsonStepOptionConverter()
          6 -> ToSeventhVersionJsonStepOptionConverter(language)
          8 -> To9VersionJsonStepOptionConverter()
          else -> null
        }
        if (converter != null) {
          convertedStepOptions = converter.convert(convertedStepOptions)
        }
        version++
      }
      convertedStepOptions.addProperty(FORMAT_VERSION, maxVersion)
      return convertedStepOptions
    }
  }
}
