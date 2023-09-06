package com.jetbrains.edu.learning.yaml.format.checkio

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOStation
import com.jetbrains.edu.learning.json.mixins.NotImplementedInMixin
import com.jetbrains.edu.learning.yaml.format.LessonBuilder
import com.jetbrains.edu.learning.yaml.format.LessonYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = StationBuilder::class)
@JsonPropertyOrder(TYPE, CUSTOM_NAME, CONTENT)
class CheckiOStationYamlMixin : LessonYamlMixin() {

  val itemType: String
    @JsonProperty(TYPE)
    get() {
      throw NotImplementedInMixin()
    }

  @Suppress("LateinitVarOverridesLateinitVar")
  @JsonIgnore
  override lateinit var contentTags: List<String>
}

@JsonPOJOBuilder(withPrefix = "")
private class StationBuilder(@JsonProperty(CONTENT) content: List<String?>) : LessonBuilder(content) {
  override fun createLesson() = CheckiOStation()
}
