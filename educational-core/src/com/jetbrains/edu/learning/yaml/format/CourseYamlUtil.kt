@file:JvmName("CourseYamlUtil")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TAGS
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduNames.EDU
import com.jetbrains.edu.learning.EduNames.PYCHARM
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_TYPE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_COURSE_TYPE
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.serialization.IntValueFilter
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_TYPE
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.unnamedItemAtMessage
import com.jetbrains.edu.learning.yaml.errorHandling.unsupportedItemTypeMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.END_DATE_TIME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_PRIVATE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.MARKETPLACE_COURSE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUBMIT_MANUALLY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUMMARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TITLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TOP_LEVEL_LESSONS_SECTION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.UPDATE_DATE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VENDOR
import java.time.ZonedDateTime
import java.util.*

/**
 * Mixin class is used to deserialize [Course] item.
 * Update [CourseChangeApplier] and [CourseBuilder] if new fields added to mixin
 */
@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
@JsonPropertyOrder(TYPE, TITLE, LANGUAGE, SUMMARY, VENDOR, IS_PRIVATE, PROGRAMMING_LANGUAGE,
                   PROGRAMMING_LANGUAGE_VERSION, ENVIRONMENT, SOLUTIONS_HIDDEN, CONTENT, FEEDBACK_LINK, TAGS)
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

  @JsonProperty(VENDOR)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private val myVendor: Vendor? = null

  @JsonProperty(IS_PRIVATE)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var isMarketplacePrivate: Boolean = false

  @JsonProperty(FEEDBACK_LINK)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected open lateinit var myFeedbackLink: String

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  protected open lateinit var contentTags: List<String>
}

@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
abstract class CourseraCourseYamlMixin : CourseYamlMixin() {
  @JsonProperty(SUBMIT_MANUALLY)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var submitManually = false

  @JsonIgnore
  override lateinit var myFeedbackLink: String

  @JsonIgnore
  override lateinit var contentTags: List<String>
}

private class ProgrammingLanguageConverter : StdConverter<String, String>() {
  override fun convert(languageId: String): String {
    val languageWithoutVersion = languageId.split(" ").first()
    return Language.findLanguageByID(languageWithoutVersion)?.displayName
           ?: formatError(EduCoreBundle.message("yaml.editor.invalid.cannot.save", languageId))
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
@JsonPropertyOrder(ID, UPDATE_DATE, TOP_LEVEL_LESSONS_SECTION, MARKETPLACE_COURSE_VERSION)
abstract class EduCourseRemoteInfoYamlMixin : RemoteStudyItemYamlMixin() {

  @JsonSerialize(converter = TopLevelLessonsSectionSerializer::class)
  @JsonDeserialize(converter = TopLevelLessonsSectionDeserializer::class)
  @JsonProperty(TOP_LEVEL_LESSONS_SECTION)
  private lateinit var sectionIds: List<Int>

  @JsonProperty(MARKETPLACE_COURSE_VERSION)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var myMarketplaceCourseVersion: Int? = 0
}

/**
 * Mixin class is used to deserialize remote information of [CodeforcesCourse] item.
 */
@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonPropertyOrder(TYPE, ID, UPDATE_DATE)
abstract class CodeforcesCourseRemoteInfoYamlMixin : RemoteStudyItemYamlMixin() {

  @JsonProperty(TYPE)
  private fun getItemType(): String {
    throw NotImplementedInMixin()
  }
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
  @JsonProperty(VENDOR) val yamlVendor: Vendor?,
  @JsonProperty(IS_PRIVATE) val yamlIsPrivate: Boolean?,
  @JsonProperty(FEEDBACK_LINK) val yamlFeedbackLink: String?,
  @JsonProperty(PROGRAMMING_LANGUAGE) val programmingLanguage: String,
  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION) val programmingLanguageVersion: String?,
  @JsonProperty(LANGUAGE) val language: String,
  @JsonProperty(ENVIRONMENT) val yamlEnvironment: String?,
  @JsonProperty(CONTENT) val content: List<String?> = emptyList(),
  @JsonProperty(SUBMIT_MANUALLY) val courseraSubmitManually: Boolean?,
  @JsonProperty(SOLUTIONS_HIDDEN) val areSolutionsHidden: Boolean?,
  @JsonProperty(END_DATE_TIME) val codeforcesEndDateTime: ZonedDateTime?,
  @JsonProperty(TAGS) val yamlContentTags: List<String> = emptyList()
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
      CODEFORCES_COURSE_TYPE -> {
        CodeforcesCourse().apply {
          endDateTime = codeforcesEndDateTime
        }
      }
      EDU -> EduCourse()
      MARKETPLACE -> {
        EduCourse().apply {
          isMarketplace = true
        }
      }
      null -> EduCourse()
      else -> formatError(unsupportedItemTypeMessage(courseType, EduNames.COURSE))
    }
    course.apply {
      name = title
      description = summary
      environment = yamlEnvironment ?: EduNames.DEFAULT_ENVIRONMENT
      vendor = yamlVendor
      isMarketplacePrivate = yamlIsPrivate ?: false
      feedbackLink = yamlFeedbackLink
      if (marketplaceCourseVersion == 0) marketplaceCourseVersion = 1
      solutionsHidden = areSolutionsHidden ?: false
      contentTags = yamlContentTags

      // for C++ there are two languages with the same display name, and we have to filter out the one we have configurator for
      val languages = Language.getRegisteredLanguages()
        .filter { it.displayName == programmingLanguage }
        .filter { EduConfiguratorManager.findConfigurator(itemType, environment, it) != null }
      if (languages.isEmpty()) {
        formatError(EduCoreBundle.message("yaml.editor.invalid.unsupported.language", programmingLanguage))
      }

      if (languages.size > 1) {
        error("Multiple configurators for language with name: $programmingLanguage")
      }

      language = languages.first().id

      val languageSettings = configurator?.courseBuilder?.getLanguageSettings()
                             ?: formatError(EduCoreBundle.message("yaml.editor.invalid.unsupported.language", programmingLanguage))
      if (programmingLanguageVersion != null) {
        if (!languageSettings.getLanguageVersions().contains(programmingLanguageVersion)) {
          formatError(EduCoreBundle.message("yaml.editor.invalid.unsupported.language.with.version", programmingLanguage,
                                            programmingLanguageVersion))
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

    val locale = Locale.getISOLanguages().find { displayLanguageByCode(it) == language } ?: formatError(
      EduCoreBundle.message("yaml.editor.invalid.format.unknown.field", language))
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
    existingItem.vendor = deserializedItem.vendor
    existingItem.feedbackLink = deserializedItem.feedbackLink
    existingItem.isMarketplacePrivate = deserializedItem.isMarketplacePrivate
    if (deserializedItem.languageVersion != null) {
      existingItem.language = "${existingItem.language} ${deserializedItem.languageVersion}"
    }
    else {
      existingItem.language = deserializedItem.language
    }
    if (deserializedItem is CourseraCourse && existingItem is CourseraCourse) {
      existingItem.submitManually = deserializedItem.submitManually
    }
    if (deserializedItem is CodeforcesCourse && existingItem is CodeforcesCourse) {
      existingItem.endDateTime = deserializedItem.endDateTime
    }
  }
}

class RemoteEduCourseChangeApplier : RemoteInfoChangeApplierBase<EduCourse>() {
  override fun applyChanges(existingItem: EduCourse, deserializedItem: EduCourse) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.sectionIds = deserializedItem.sectionIds
    existingItem.marketplaceCourseVersion = deserializedItem.marketplaceCourseVersion
  }
}

class RemoteHyperskillChangeApplier : RemoteInfoChangeApplierBase<HyperskillCourse>() {
  override fun applyChanges(existingItem: HyperskillCourse, deserializedItem: HyperskillCourse) {
    existingItem.hyperskillProject = deserializedItem.hyperskillProject
    existingItem.stages = deserializedItem.stages
    existingItem.taskToTopics = deserializedItem.taskToTopics
    existingItem.updateDate = deserializedItem.updateDate
  }
}