package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.stepik.hyperskill.api.*
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HYPERSKILL_PROJECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.MODE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUMMARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TITLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
@JsonPropertyOrder(TYPE, TITLE, LANGUAGE, SUMMARY, PROGRAMMING_LANGUAGE, PROGRAMMING_LANGUAGE_VERSION, ENVIRONMENT, CONTENT, MODE)
abstract class StudentCourseYamlMixin : CourseYamlMixin() {
  @JsonProperty(MODE)
  private var courseMode: String = EduNames.STUDY
}

@JsonPropertyOrder(TYPE, TITLE, LANGUAGE, SUMMARY, PROGRAMMING_LANGUAGE, PROGRAMMING_LANGUAGE_VERSION, ENVIRONMENT, CONTENT, MODE, HYPERSKILL_PROJECT)
abstract class HyperskillCourseMixin: StudentCourseYamlMixin() {

  @JsonProperty(HYPERSKILL_PROJECT)
  lateinit var hyperskillProject: HyperskillProject
}

@Suppress("unused")
@JsonPropertyOrder(ID, IDE_FILES, IS_TEMPLATE_BASED)
abstract class HyperskillProjectMixin {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonIgnore
  var title: String = ""

  @JsonIgnore
  var description: String = ""

  @JsonProperty(IDE_FILES)
  var ideFiles: String = ""

  @JsonIgnore
  var useIde: Boolean = false

  @JsonIgnore
  var language: String = ""

  @JsonProperty(IS_TEMPLATE_BASED)
  var isTemplateBased: Boolean = false
}