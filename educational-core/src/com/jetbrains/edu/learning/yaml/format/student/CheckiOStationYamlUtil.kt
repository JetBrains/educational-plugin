package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.yaml.format.LessonBuilder
import com.jetbrains.edu.learning.yaml.format.LessonYamlMixin
import com.jetbrains.edu.learning.yaml.format.NotImplementedInMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = StationBuilder::class)
@JsonPropertyOrder(TYPE, CUSTOM_NAME, CONTENT)
class CheckiOStationYamlMixin : LessonYamlMixin() {
  @JsonProperty(TYPE)
  private fun getItemType(): String {
    throw NotImplementedInMixin()
  }
}

@JsonPOJOBuilder(withPrefix = "")
private class StationBuilder(@JsonProperty(CONTENT) content: List<String?>) : LessonBuilder(content) {
  override fun createLesson() = CheckiOStation()
}
