@file:JvmName("CourseYamlUtil")

package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.yaml.formatError
import com.jetbrains.edu.coursecreator.yaml.unknownFieldValueMessage
import com.jetbrains.edu.coursecreator.yaml.unnamedItemAtMessage
import com.jetbrains.edu.coursecreator.yaml.unsupportedItemTypeMessage
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_TYPE
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_TYPE
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.util.*

private const val TYPE = "type"
private const val TITLE = "title"
private const val LANGUAGE = "language"
private const val SUMMARY = "summary"
private const val PROGRAMMING_LANGUAGE = "programming_language"
private const val CONTENT = "content"
private const val ENVIRONMENT = "environment"
private const val PROGRAMMING_LANGUAGE_VERSION = "programming_language_version"

private const val TOP_LEVEL_LESSONS_SECTION = "default_section"

/**
 * Mixin class is used to deserialize [Course] item.
 * Update [CourseChangeApplier] and [CourseBuilder] if new fields added to mixin
 */
@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
@JsonPropertyOrder(TITLE, LANGUAGE, SUMMARY, PROGRAMMING_LANGUAGE, PROGRAMMING_LANGUAGE_VERSION, ENVIRONMENT, CONTENT)
@JsonDeserialize(builder = CourseBuilder::class)
abstract class CourseYamlMixin {
  @JsonSerialize(converter = CourseTypeSerializationConverter::class)
  @JsonProperty(TYPE)
  fun getItemType(): String {
    throw NotImplementedInMixin()
  }

  @JsonProperty(TITLE)
  private lateinit var myName: String

  @JsonProperty(SUMMARY)
  private lateinit var description: String

  @JsonSerialize(converter = ProgrammingLanguageConverter::class)
  @JsonProperty(PROGRAMMING_LANGUAGE)
  private lateinit var myProgrammingLanguage: String

  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION)
  fun getLanguageVersion(): String? {
    throw NotImplementedInMixin()
  }

  @JsonSerialize(converter = LanguageConverter::class)
  @JsonProperty(LANGUAGE)
  private lateinit var myLanguageCode: String

  @JsonProperty(ENVIRONMENT)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  lateinit var myEnvironment: String

  @JsonSerialize(contentConverter = StudyItemConverter::class)
  @JsonProperty(CONTENT)
  private lateinit var items: List<StudyItem>
}

private class ProgrammingLanguageConverter : StdConverter<String, String>() {
  override fun convert(languageId: String): String {
    val languageWithoutVersion = languageId.split(" ").first()
    return Language.findLanguageByID(languageWithoutVersion)?.displayName ?: formatError("Cannot save programming language '$languageId'")
  }
}

private class LanguageConverter : StdConverter<String, String>() {
  override fun convert(languageCode: String): String = Locale(languageCode).displayName
}

private class CourseTypeSerializationConverter : StdConverter<String, String?>() {
  override fun convert(courseType: String): String? {
    return if (courseType == EduNames.PYCHARM) null else courseType
  }
}

/**
 * Mixin class is used to deserialize remote information of [EduCourse] item stored on Stepik.
 */
@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonPropertyOrder(ID, UPDATE_DATE, TOP_LEVEL_LESSONS_SECTION)
abstract class EduCourseRemoteInfoYamlMixin : RemoteStudyItemYamlMixin() {

  @JsonSerialize(converter = TopLevelLessonsSectionSerializer::class)
  @JsonDeserialize(converter = TopLevelLessonsSectionDeserializer::class)
  @JsonProperty(TOP_LEVEL_LESSONS_SECTION)
  private lateinit var sectionIds: List<Int>
}

private class TopLevelLessonsSectionSerializer : StdConverter<List<Int>, Int?>() {
  override fun convert(value: List<Int>?) = value?.firstOrNull()
}

private class TopLevelLessonsSectionDeserializer : StdConverter<Int, List<Int>>() {
  override fun convert(value: Int?) = if (value == null) emptyList() else listOf(value)
}

@JsonPOJOBuilder(withPrefix = "")
private class CourseBuilder(@JsonProperty(TYPE) val courseType: String?,
                            @JsonProperty(TITLE) val title: String,
                            @JsonProperty(SUMMARY) val summary: String,
                            @JsonProperty(PROGRAMMING_LANGUAGE) val programmingLanguage: String,
                            @JsonProperty(PROGRAMMING_LANGUAGE_VERSION) val programmingLanguageVersion: String?,
                            @JsonProperty(LANGUAGE) val language: String,
                            @JsonProperty(ENVIRONMENT) val yamlEnvironment: String?,
                            @JsonProperty(CONTENT) val content: List<String?> = emptyList()) {
  @Suppress("unused") // used for deserialization
  private fun build(): Course {
    val course = when (courseType) {
      CourseraNames.COURSE_TYPE -> CourseraCourse()
      CHECKIO_TYPE -> CheckiOCourse()
      HYPERSKILL_TYPE -> HyperskillCourse()
      STEPIK_TYPE -> StepikCourse()
      null -> EduCourse()
      else -> formatError(unsupportedItemTypeMessage(courseType, EduNames.COURSE))
    }
    course.apply {
      name = title
      description = summary
      val languageName = Language.getRegisteredLanguages().find { it.displayName == programmingLanguage }
                         ?: formatError(unknownFieldValueMessage("programming language", programmingLanguage))
      environment = yamlEnvironment ?: EduNames.DEFAULT_ENVIRONMENT
      language = languageName.id

      val languageSettings = configurator?.courseBuilder?.languageSettings ?: formatError("Unsupported language $languageName")
      if (programmingLanguageVersion != null) {
        if (!languageSettings.languageVersions.contains(programmingLanguageVersion)) {
          formatError("Unsupported ${languageName.displayName} version: $programmingLanguageVersion")
        }
        else {
          language = "$language $programmingLanguageVersion"
        }
      }
      val items = content.mapIndexed { index, title ->
        if (title == null) {
          formatError(unnamedItemAtMessage(index + 1))
        }
        val titledStudyItem = TitledStudyItem(title)
        titledStudyItem.index = index + 1
        titledStudyItem
      }
      setItems(items)
    }
    val locale = Locale.getISOLanguages().find { Locale(it).displayLanguage == language } ?: formatError(
      unknownFieldValueMessage("language", language))
    course.languageCode = Locale(locale).language
    return course
  }
}

class CourseChangeApplier(project: Project) : ItemContainerChangeApplier<Course>(project) {
  override fun applyChanges(existingItem: Course, deserializedItem: Course) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.name = deserializedItem.name
    existingItem.description = deserializedItem.description
    existingItem.languageCode = deserializedItem.languageCode
    existingItem.environment = deserializedItem.environment
    if (deserializedItem.languageVersion != null) {
      existingItem.language = "${existingItem.language} ${deserializedItem.languageVersion}"
    }
    else {
      existingItem.language = deserializedItem.language
    }
  }
}

class RemoteCourseChangeApplier : RemoteInfoChangeApplierBase<EduCourse>() {
  override fun applyChanges(existingItem: EduCourse, deserializedItem: EduCourse) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.sectionIds = deserializedItem.sectionIds
  }
}