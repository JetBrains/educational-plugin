@file:JvmName("CourseYamlUtil")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduNames.EDU
import com.jetbrains.edu.learning.EduNames.PYCHARM
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_TYPE
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
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
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.unknownFieldValueMessage
import com.jetbrains.edu.learning.yaml.errorHandling.unnamedItemAtMessage
import com.jetbrains.edu.learning.yaml.errorHandling.unsupportedItemTypeMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUBMIT_MANUALLY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUMMARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TITLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TOP_LEVEL_LESSONS_SECTION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.UPDATE_DATE
import java.util.*

/**
 * Mixin class is used to deserialize [Course] item.
 * Update [CourseChangeApplier] and [CourseBuilder] if new fields added to mixin
 */
@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
@JsonPropertyOrder(TYPE, TITLE, LANGUAGE, SUMMARY, PROGRAMMING_LANGUAGE, PROGRAMMING_LANGUAGE_VERSION, ENVIRONMENT, SOLUTIONS_HIDDEN,
                   CONTENT)
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
  private lateinit var myEnvironment: String

  @JsonSerialize(contentConverter = StudyItemConverter::class)
  @JsonProperty(CONTENT)
  private lateinit var items: List<StudyItem>

  @JsonProperty(SOLUTIONS_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var solutionsHidden: Boolean = false
}

@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
abstract class CourseraCourseYamlMixin : CourseYamlMixin() {
  @JsonProperty(SUBMIT_MANUALLY)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var submitManually = false
}

private class ProgrammingLanguageConverter : StdConverter<String, String>() {
  override fun convert(languageId: String): String {
    val languageWithoutVersion = languageId.split(" ").first()
    return Language.findLanguageByID(languageWithoutVersion)?.displayName ?: formatError("Cannot save programming language '$languageId'")
  }
}

private class LanguageConverter : StdConverter<String, String>() {
  override fun convert(languageCode: String): String = displayLanguageByCode(languageCode)
}

private class CourseTypeSerializationConverter : StdConverter<String, String?>() {
  override fun convert(courseType: String): String? {
    return if (courseType == PYCHARM) null else courseType.decapitalize()
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
private class CourseBuilder(
  @JsonProperty(TYPE) val courseType: String?,
  @JsonProperty(TITLE) val title: String,
  @JsonProperty(SUMMARY) val summary: String,
  @JsonProperty(PROGRAMMING_LANGUAGE) val programmingLanguage: String,
  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION) val programmingLanguageVersion: String?,
  @JsonProperty(LANGUAGE) val language: String,
  @JsonProperty(ENVIRONMENT) val yamlEnvironment: String?,
  @JsonProperty(CONTENT) val content: List<String?> = emptyList(),
  @JsonProperty(SUBMIT_MANUALLY) val courseraSubmitManually: Boolean?,
  @JsonProperty(SOLUTIONS_HIDDEN) val areSolutionsHidden: Boolean?
) {
  @Suppress("unused") // used for deserialization
  private fun build(): Course {
    val course = when (courseType?.capitalize()) {
      CourseraNames.COURSE_TYPE -> {
        CourseraCourse().apply {
          submitManually = courseraSubmitManually ?: false
        }
      }
      CHECKIO_TYPE -> CheckiOCourse()
      HYPERSKILL_TYPE -> HyperskillCourse()
      STEPIK_TYPE -> StepikCourse()
      EDU -> EduCourse()
      null -> EduCourse()
      else -> formatError(unsupportedItemTypeMessage(courseType, EduNames.COURSE))
    }
    course.apply {
      name = title
      description = summary
      environment = yamlEnvironment ?: EduNames.DEFAULT_ENVIRONMENT
      solutionsHidden = areSolutionsHidden ?: false

      // for C++ there are two languages with the same display name, and we have to filter out the one we have configurator for
      val languages = Language.getRegisteredLanguages()
        .filter { it.displayName == programmingLanguage }
        .filter { EduConfiguratorManager.findConfigurator(itemType, environment, it) != null }
      if (languages.isEmpty()) {
        formatError("Unsupported language $programmingLanguage")
      }

      if (languages.size > 1) {
        error("Multiple configurators for language with name: $programmingLanguage")
      }

      language = languages.first().id

      val languageSettings = configurator?.courseBuilder?.getLanguageSettings()
                             ?: formatError("Unsupported language $programmingLanguage")
      if (programmingLanguageVersion != null) {
        if (!languageSettings.languageVersions.contains(programmingLanguageVersion)) {
          formatError("Unsupported $programmingLanguage version: $programmingLanguageVersion")
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

    val locale = Locale.getISOLanguages().find { displayLanguageByCode(it) == language }
                 ?: formatError(unknownFieldValueMessage("language", language))
    course.languageCode = Locale(locale).language
    return course
  }
}

private fun displayLanguageByCode(languageCode: String) = Locale(languageCode).getDisplayLanguage(Locale.ENGLISH)

class CourseChangeApplier(project: Project) : ItemContainerChangeApplier<Course>(project) {
  override fun applyChanges(existingItem: Course, deserializedItem: Course) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.name = deserializedItem.name
    existingItem.description = deserializedItem.description
    existingItem.languageCode = deserializedItem.languageCode
    existingItem.environment = deserializedItem.environment
    existingItem.solutionsHidden = deserializedItem.solutionsHidden
    if (deserializedItem.languageVersion != null) {
      existingItem.language = "${existingItem.language} ${deserializedItem.languageVersion}"
    }
    else {
      existingItem.language = deserializedItem.language
    }
    if (deserializedItem is CourseraCourse && existingItem is CourseraCourse) {
      existingItem.submitManually = deserializedItem.submitManually
    }
  }
}

class RemoteCourseChangeApplier : RemoteInfoChangeApplierBase<EduCourse>() {
  override fun applyChanges(existingItem: EduCourse, deserializedItem: EduCourse) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.sectionIds = deserializedItem.sectionIds
  }
}

class RemoteHyperskillChangeApplier: RemoteInfoChangeApplierBase<HyperskillCourse>() {
  override fun applyChanges(existingItem: HyperskillCourse, deserializedItem: HyperskillCourse) {
    existingItem.hyperskillProject = deserializedItem.hyperskillProject
    existingItem.stages = deserializedItem.stages
    existingItem.taskToTopics = deserializedItem.taskToTopics
  }
}