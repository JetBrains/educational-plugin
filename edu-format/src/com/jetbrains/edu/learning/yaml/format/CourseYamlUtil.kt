@file:JvmName("CourseYamlUtil")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonAppend
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedClass
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter
import com.fasterxml.jackson.databind.util.Annotations
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYCHARM
import com.jetbrains.edu.learning.json.mixins.IntValueFilter
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.CUSTOM_CONTENT_PATH
import com.jetbrains.edu.learning.json.mixins.NotImplementedInMixin
import com.jetbrains.edu.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.unnamedItemAtMessage
import com.jetbrains.edu.learning.yaml.errorHandling.unsupportedItemTypeMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDU_YAML_TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT_SETTINGS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.GENERATED_EDU_ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_PRIVATE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.MARKETPLACE_COURSE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.MARKETPLACE_YAML_TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUBMIT_MANUALLY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUMMARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TITLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TOP_LEVEL_LESSONS_SECTION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.UPDATE_DATE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VENDOR
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.YAML_VERSION
import com.jetbrains.edu.learning.yaml.format.remote.RemoteStudyItemYamlMixin
import java.util.*

/**
 * Mixin class is used to deserialize [Course] item.
 * Update [com.jetbrains.edu.learning.yaml.format.CourseChangeApplier] and [CourseBuilder] if new fields added to mixin
 */
@Suppress("unused") // used for yaml serialization
@JsonAppend(
  props = [
    JsonAppend.Prop(YamlVersionWriter::class, name=YAML_VERSION)
  ]
)
@JsonPropertyOrder(
  TYPE,
  TITLE,
  LANGUAGE,
  SUMMARY,
  VENDOR,
  IS_PRIVATE,
  PROGRAMMING_LANGUAGE,
  PROGRAMMING_LANGUAGE_VERSION,
  ENVIRONMENT,
  SOLUTIONS_HIDDEN,
  CONTENT,
  FEEDBACK_LINK,
  TAGS,
  ENVIRONMENT_SETTINGS,
  ADDITIONAL_FILES,
  CUSTOM_CONTENT_PATH
  // YAML_VERSION is appended to the end with the @JsonAppend annotation
)
@JsonDeserialize(builder = CourseBuilder::class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = TYPE, defaultImpl = EduCourse::class, visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(EduCourse::class, name = "edu"),
  JsonSubTypes.Type(EduCourse::class, name = "marketplace")
)
abstract class CourseYamlMixin {
  val itemType: String
    @JsonSerialize(converter = CourseTypeSerializationConverter::class)
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(SUMMARY)
  private lateinit var description: String

  @JsonSerialize(converter = ProgrammingLanguageConverter::class)
  @JsonProperty(PROGRAMMING_LANGUAGE)
  lateinit var languageId: String

  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION)
  private var languageVersion: String? = null

  @JsonSerialize(converter = LanguageConverter::class)
  @JsonProperty(LANGUAGE)
  private lateinit var languageCode: String

  @JsonProperty(ENVIRONMENT)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var environment: String

  @JsonSerialize(contentConverter = StudyItemConverter::class)
  @JsonProperty(CONTENT)
  private lateinit var _items: List<StudyItem>

  @JsonProperty(SOLUTIONS_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var solutionsHidden: Boolean = false

  @JsonProperty(VENDOR)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private val vendor: Vendor? = null

  @JsonProperty(IS_PRIVATE)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var isMarketplacePrivate: Boolean = false

  @JsonProperty(FEEDBACK_LINK)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected open lateinit var feedbackLink: String

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  protected open lateinit var contentTags: List<String>

  @JsonProperty(ENVIRONMENT_SETTINGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  lateinit var environmentSettings: Map<String, String>

  @JsonProperty(ADDITIONAL_FILES)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  lateinit var additionalFiles: List<EduFile>

  @JsonProperty(CUSTOM_CONTENT_PATH)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  lateinit var customContentPath: String

  @JsonIgnore
  private var programmingLanguage: String? = null
}

private class YamlVersionWriter : VirtualBeanPropertyWriter {
  override fun value(bean: Any?, gen: JsonGenerator?, prov: SerializerProvider?): Any = CURRENT_YAML_VERSION

  @Suppress("unused")
  constructor()

  constructor(
    propDef: BeanPropertyDefinition?,
    contextAnnotations: Annotations?,
    declaredType: JavaType?
  ) : super(propDef, contextAnnotations, declaredType)

  override fun withConfig(
    config: MapperConfig<*>?,
    declaringClass: AnnotatedClass?,
    propDef: BeanPropertyDefinition?,
    type: JavaType?
  ): VirtualBeanPropertyWriter {
    return YamlVersionWriter(propDef, declaringClass?.annotations, type)
  }
}

private class ProgrammingLanguageConverter : StdConverter<String, String>() {
  override fun convert(languageId: String): String {
    return Language.findLanguageByID(languageId)
           ?: formatError(message("yaml.editor.invalid.cannot.save", languageId))
  }
}

private class LanguageConverter : StdConverter<String, String>() {
  override fun convert(languageCode: String): String = displayLanguageByCode(languageCode)
}

private class CourseTypeSerializationConverter : StdConverter<String, String?>() {
  override fun convert(courseType: String): String? {
    return if (courseType == PYCHARM) null else courseType.replaceFirstChar { it.lowercaseChar() }
  }
}

/**
 * Mixin class is used to deserialize remote information of [EduCourse] item
 */
@Suppress("unused") // used for json serialization
@JsonPropertyOrder(ID, UPDATE_DATE, TOP_LEVEL_LESSONS_SECTION, MARKETPLACE_COURSE_VERSION, GENERATED_EDU_ID)
abstract class EduCourseRemoteInfoYamlMixin : RemoteStudyItemYamlMixin() {

  @JsonSerialize(converter = TopLevelLessonsSectionSerializer::class)
  @JsonDeserialize(converter = TopLevelLessonsSectionDeserializer::class)
  @JsonProperty(TOP_LEVEL_LESSONS_SECTION)
  private lateinit var sectionIds: List<Int>   // applicable only to Stepik courses. To be removed

  @JsonProperty(MARKETPLACE_COURSE_VERSION)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var marketplaceCourseVersion: Int? = 0

  @JsonProperty(GENERATED_EDU_ID)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private val generatedEduId: String? = null
}


private class TopLevelLessonsSectionSerializer : StdConverter<List<Int>, Int?>() {
  override fun convert(value: List<Int>?) = value?.firstOrNull()
}

private class TopLevelLessonsSectionDeserializer : StdConverter<Int, List<Int>>() {
  override fun convert(value: Int?) = if (value == null) emptyList() else listOf(value)
}

@JsonPOJOBuilder(withPrefix = "")
open class CourseBuilder(
  @JsonProperty(TYPE) val courseType: String?,
  @JsonProperty(TITLE) val title: String,
  @JsonProperty(SUMMARY) val summary: String?,
  @JsonProperty(VENDOR) val yamlVendor: Vendor?,
  @JsonProperty(IS_PRIVATE) val yamlIsPrivate: Boolean?,
  @JsonProperty(FEEDBACK_LINK) val yamlFeedbackLink: String?,
  @JsonProperty(GENERATED_EDU_ID) val yamlGeneratedEduId: String?,
  @JsonProperty(PROGRAMMING_LANGUAGE) val displayProgrammingLanguageName: String,
  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION) val programmingLanguageVersion: String?,
  @JsonProperty(LANGUAGE) val language: String,
  @JsonProperty(ENVIRONMENT) val yamlEnvironment: String?,
  @JsonProperty(CONTENT) val content: List<String?> = emptyList(),
  @JsonProperty(SUBMIT_MANUALLY) val courseraSubmitManually: Boolean?,
  @JsonProperty(SOLUTIONS_HIDDEN) val areSolutionsHidden: Boolean?,
  @JsonProperty(TAGS) val yamlContentTags: List<String> = emptyList(),
  @JsonProperty(ENVIRONMENT_SETTINGS) val yamlEnvironmentSettings: Map<String, String> = emptyMap(),
  @JsonProperty(ADDITIONAL_FILES) val yamlAdditionalFiles: List<EduFile> = emptyList(),
  @JsonProperty(CUSTOM_CONTENT_PATH) val pathToContent: String = ""
) {
  @Suppress("unused") // used for deserialization
  private fun build(): Course {
    val course = makeCourse() ?: formatError(unsupportedItemTypeMessage(courseType ?: "", EduFormatNames.COURSE))
    course.apply {
      name = title
      description = summary ?: ""
      environment = yamlEnvironment ?: DEFAULT_ENVIRONMENT
      vendor = yamlVendor
      isMarketplacePrivate = yamlIsPrivate ?: false
      feedbackLink = yamlFeedbackLink
      if (marketplaceCourseVersion == 0) marketplaceCourseVersion = 1
      solutionsHidden = areSolutionsHidden ?: false
      contentTags = yamlContentTags
      environmentSettings = yamlEnvironmentSettings
      additionalFiles = yamlAdditionalFiles

      languageId = Language.findLanguageByName(displayProgrammingLanguageName)
                      ?: formatError(message("yaml.editor.invalid.unsupported.language", displayProgrammingLanguageName))
      languageVersion = programmingLanguageVersion

      val newItems = content.mapIndexed { index, title ->
        if (title == null) {
          formatError(unnamedItemAtMessage(index + 1))
        }
        val titledStudyItem = TitledStudyItem(title)
        titledStudyItem.index = index + 1
        titledStudyItem
      }
      items = newItems
      customContentPath = pathToContent
    }

    val locale = Locale.getISOLanguages().find { displayLanguageByCode(it) == language } ?: formatError(
      message("yaml.editor.invalid.format.unknown.field", language)
    )
    course.languageCode = Locale(locale).language
    return course
  }

  open fun makeCourse(): Course? {
    val course = when (courseType) {
      MARKETPLACE_YAML_TYPE -> {
        EduCourse().apply {
          isMarketplace = true
          generatedEduId = yamlGeneratedEduId
        }
      }
      EDU_YAML_TYPE, null -> EduCourse()
      else -> formatError(unsupportedItemTypeMessage(courseType, EduFormatNames.COURSE))
    }
    return course
  }
}

private fun displayLanguageByCode(languageCode: String) = Locale(languageCode).getDisplayLanguage(Locale.ENGLISH)
