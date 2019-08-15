package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin.Companion.CONTENT
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin.Companion.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin.Companion.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin.Companion.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin.Companion.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin.Companion.SUMMARY
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin.Companion.TITLE

private const val MODE = "mode"

@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
@JsonPropertyOrder(CourseYamlMixin.TYPE, TITLE, LANGUAGE, SUMMARY, PROGRAMMING_LANGUAGE, PROGRAMMING_LANGUAGE_VERSION, ENVIRONMENT, CONTENT,
                   MODE)
class StudentCourseYamlMixin : CourseYamlMixin() {
  @JsonProperty(MODE)
  var courseMode: String = EduNames.STUDY
}