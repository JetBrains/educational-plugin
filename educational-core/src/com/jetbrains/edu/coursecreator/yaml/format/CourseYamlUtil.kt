@file:JvmName("CourseYamlUtil")

package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.yaml.InvalidYamlFormatException
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import java.util.*

private const val TYPE = "type"
private const val TITLE = "title"
private const val LANGUAGE = "language"
private const val SUMMARY = "summary"
private const val PROGRAMMING_LANGUAGE = "programming_language"
private const val CONTENT = "content"

@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(TITLE, LANGUAGE, SUMMARY, PROGRAMMING_LANGUAGE, CONTENT)
@JsonDeserialize(builder = CourseBuilder::class)
abstract class CourseYamlMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(SUMMARY)
  private lateinit var description: String

  @JsonSerialize(converter = ProgrammingLanguageConverter::class)
  @JsonProperty(PROGRAMMING_LANGUAGE)
  private lateinit var myProgrammingLanguage: String

  @JsonSerialize(converter = LanguageConverter::class)
  @JsonProperty(LANGUAGE)
  private lateinit var myLanguageCode: String

  @JsonSerialize(contentConverter = StudyItemConverter::class)
  @JsonProperty(CONTENT)
  private lateinit var items: List<StudyItem>
}

private class ProgrammingLanguageConverter : StdConverter<String, String>() {
  override fun convert(languageId: String): String {
    val languageWithoutVersion = languageId.split(" ").first()
    return Language.findLanguageByID(languageWithoutVersion)?.displayName
           ?: throw InvalidYamlFormatException("Cannot save programming language: ${languageId}")
  }
}

private class LanguageConverter : StdConverter<String, String>() {
  override fun convert(languageCode: String): String = Locale(languageCode).displayName
}

@JsonPOJOBuilder(withPrefix = "")
private class CourseBuilder(@JsonProperty(TITLE) val title: String,
                            @JsonProperty(SUMMARY) val summary: String,
                            @JsonProperty(PROGRAMMING_LANGUAGE) val programmingLanguage: String,
                            @JsonProperty(LANGUAGE) val language: String,
                            @JsonProperty(CONTENT) val content: List<String?>) {
  @Suppress("unused") // used for deserialization
  private fun build(): Course {
    val course = EduCourse()
    course.apply {
      name = title
      description = summary
      val languageName = Language.getRegisteredLanguages().find { it.displayName == programmingLanguage }
      if (languageName == null) {
        throw InvalidYamlFormatException(
          "Unknown programming language '$programmingLanguage'")
      }
      language = languageName.id
      val items = content.map {
        if (it == null) {
          throw InvalidYamlFormatException("Unnamed item")
        }
        TitledStudyItem(it)
      }
      setItems(items)
    }
    val locale = Locale.getISOLanguages().find { Locale(it).displayLanguage == language }
    if (locale == null) {
      throw InvalidYamlFormatException("Unknown language '$language'")
    }
    course.languageCode = Locale(locale).language
    return course
  }
}