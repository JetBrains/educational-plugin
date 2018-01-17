@file:JvmName("CourseYamlUtil")

package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.configuration.InvalidYamlFormatException
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder("title", "language", "summary", "programming_language", "lessons")
@JsonDeserialize(builder = CourseBuilder::class)
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

  @JsonSerialize(contentConverter = LessonConverter::class)
  @JsonProperty("lessons")
  private lateinit var items: List<StudyItem>
}

private class ProgrammingLanguageConverter : StdConverter<String, String>() {
  override fun convert(languageId: String): String = Language.findLanguageByID(languageId)!!.displayName
}

private class LanguageConverter : StdConverter<String, String>() {
  override fun convert(languageCode: String): String = Locale(languageCode).displayName
}

private class LessonConverter : StdConverter<Lesson, String>() {
  override fun convert(lesson: Lesson): String = lesson.name
}

@JsonPOJOBuilder(withPrefix = "")
private class CourseBuilder(@JsonProperty("title") val title: String,
                            @JsonProperty("summary") val summary: String,
                            @JsonProperty("programming_language") val programmingLanguage: String,
                            @JsonProperty("language") val language: String,
                            @JsonProperty("lessons") val lessonNames: List<String?>) {
  @Suppress("unused") // used for deserialization
  private fun build(): Course {
    val course = Course()
    course.apply {
      name = title
      description = summary
      val languageName = Language.getRegisteredLanguages().find { it.displayName == programmingLanguage }
      if (languageName == null) {
        throw InvalidYamlFormatException(
          "Unknown programming language '$programmingLanguage'")
      }
      language = languageName.id
      val lessons = lessonNames.map {
        if (it == null) {
          throw InvalidYamlFormatException("Unnamed lesson")
        }
        val lesson = Lesson()
        lesson.name = it
        lesson
      }
      lessons.forEach { this.addLesson(it) }
    }
    val locale = Locale.getISOLanguages().find { Locale(it).displayLanguage == language }
    if (locale == null) {
      throw InvalidYamlFormatException("Unknown language '$language'")
    }
    course.languageCode = Locale(locale).language
    return course
  }
}