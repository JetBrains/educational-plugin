package com.jetbrains.edu.learning.yaml.format.coursera

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.remote.RemoteCourseBuilder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUBMIT_MANUALLY

@Suppress("unused", "LateinitVarOverridesLateinitVar") // used for yaml serialization
@JsonDeserialize(builder = RemoteCourseBuilder::class)
abstract class CourseraCourseYamlMixin : CourseYamlMixin() {
  @JsonProperty(SUBMIT_MANUALLY)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var submitManually = false

  @JsonIgnore
  override lateinit var feedbackLink: String

  @JsonIgnore
  override lateinit var contentTags: List<String>

}