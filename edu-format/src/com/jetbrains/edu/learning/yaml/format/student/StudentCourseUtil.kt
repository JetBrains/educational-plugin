package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.MODE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_CONTENT_PATH
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUMMARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TITLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(
  TYPE,
  TITLE,
  LANGUAGE,
  SUMMARY,
  PROGRAMMING_LANGUAGE,
  PROGRAMMING_LANGUAGE_VERSION,
  ENVIRONMENT,
  CONTENT,
  CUSTOM_CONTENT_PATH,
  ADDITIONAL_FILES,
  MODE,
  TAGS
)
abstract class StudentCourseYamlMixin : CourseYamlMixin() {
  @JsonSerialize(converter = CourseModeSerializationConverter::class)
  @JsonProperty(MODE)
  private var courseMode = CourseMode.STUDENT
}

private class CourseModeSerializationConverter : StdConverter<CourseMode, String>() {
  override fun convert(courseMode: CourseMode): String {
    return courseMode.toString()
  }
}
