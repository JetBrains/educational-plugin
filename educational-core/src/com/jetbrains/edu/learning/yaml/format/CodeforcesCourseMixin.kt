package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.END_DATE_TIME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAM_TYPE_ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUMMARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TITLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import java.time.ZonedDateTime

@Suppress("unused", "LateinitVarOverridesLateinitVar") // used for yaml serialization
@JsonPropertyOrder(TYPE, TITLE, LANGUAGE, SUMMARY, PROGRAMMING_LANGUAGE, PROGRAMMING_LANGUAGE_VERSION, ENVIRONMENT, CONTENT, END_DATE_TIME, PROGRAM_TYPE_ID)
abstract class CodeforcesCourseYamlMixin : CourseYamlMixin() {
  @JsonProperty(END_DATE_TIME)
  private var endDateTime: ZonedDateTime? = null

  @JsonProperty(PROGRAM_TYPE_ID)
  private var programTypeId: String? = null

  @JsonIgnore
  override lateinit var feedbackLink: String

  @JsonIgnore
  override lateinit var contentTags: List<String>
}