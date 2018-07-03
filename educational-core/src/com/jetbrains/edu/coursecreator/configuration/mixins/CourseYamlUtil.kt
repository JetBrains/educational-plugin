@file:JvmName("CourseYamlUtil")

package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.StudyItem
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder("title", "language", "summary", "programming_language", "lessons")
abstract class CourseYamlMixin {
  @JsonProperty("title")
  private lateinit var name: String

  @JsonProperty("summary")
  private lateinit var description: String

  @JsonSerialize(converter = ProgrammingLanguageConverter::class)
  @JsonProperty("programming_language")
  private lateinit var myProgrammingLanguage: String

  @JsonSerialize(converter = LanguageConverter::class)
  @JsonProperty("language")
  private lateinit var myLanguageCode: String

  @JsonSerialize(contentConverter = StudyItemConverter::class)
  @JsonProperty("content")
  private lateinit var items: List<StudyItem>
}

private class ProgrammingLanguageConverter : StdConverter<String, String>() {
  override fun convert(languageId: String): String = Language.findLanguageByID(languageId)!!.displayName
}

private class LanguageConverter : StdConverter<String, String>() {
  override fun convert(languageCode: String): String = Locale(languageCode).displayName
}