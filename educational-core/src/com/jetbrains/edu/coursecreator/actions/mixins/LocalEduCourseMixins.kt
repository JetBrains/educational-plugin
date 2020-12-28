@file:JvmName("LocalEduCourseMixins")

package com.jetbrains.edu.coursecreator.actions.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonAppend
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.introspect.AnnotatedClass
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter
import com.fasterxml.jackson.databind.util.Annotations
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.AUTHORS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.CUSTOM_NAME
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.DEPENDENCY
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.DESCRIPTION_FORMAT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.DESCRIPTION_TEXT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ENVIRONMENT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.FILES
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.IS_TEMPLATE_BASED
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.IS_VISIBLE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ITEMS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.LANGUAGE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.LENGTH
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.LINK
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.LINK_TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.NAME
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.OFFSET
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLACEHOLDERS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLACEHOLDER_TEXT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.POSSIBLE_ANSWER
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SOLUTION_HIDDEN
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SUBMIT_MANUALLY
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SUMMARY
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TASK_LIST
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TASK_TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TEXT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TITLE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.VERSION
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.encrypt.Encrypt
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.FRAMEWORK_TYPE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.ITEM_TYPE
import com.jetbrains.edu.learning.serialization.TrueValueFilter
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.api.doDeserializeTask
import com.jetbrains.edu.learning.yaml.format.NotImplementedInMixin
import java.util.*


@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonPropertyOrder(VERSION, ENVIRONMENT, SUMMARY, TITLE, AUTHORS, PROGRAMMING_LANGUAGE, LANGUAGE, COURSE_TYPE, SOLUTIONS_HIDDEN, ITEMS)
@JsonAppend(props = [JsonAppend.Prop(VersionPropertyWriter::class, name = VERSION, type = Int::class)])
abstract class LocalEduCourseMixin {
  @JsonProperty(TITLE)
  private lateinit var myName: String

  @JsonProperty(AUTHORS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonSerialize(contentConverter = StepikUserInfoToString::class)
  @JsonDeserialize(contentConverter = StepikUserInfoFromString::class)
  private var authors: MutableList<StepikUserInfo> = ArrayList()

  @JsonProperty(SUMMARY)
  private lateinit var description: String

  @JsonProperty(PROGRAMMING_LANGUAGE)
  private lateinit var myProgrammingLanguage: String

  @JsonProperty(LANGUAGE)
  private lateinit var myLanguageCode: String

  @JsonProperty(ENVIRONMENT)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  lateinit var myEnvironment: String

  @JsonProperty(COURSE_TYPE)
  fun getItemType(): String {
    throw NotImplementedInMixin()
  }

  @JsonProperty(ITEMS)
  private lateinit var items: List<StudyItem>

  @JsonProperty(ADDITIONAL_FILES)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var additionalFiles: List<TaskFile>

  @JsonProperty(SOLUTIONS_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var solutionsHidden: Boolean = false
}

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(setterVisibility = JsonAutoDetect.Visibility.NONE)
abstract class CourseraCourseMixin : LocalEduCourseMixin() {
  @JsonProperty(SUBMIT_MANUALLY)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var submitManually = false
}

private class StepikUserInfoToString : StdConverter<StepikUserInfo, String?>() {
  override fun convert(value: StepikUserInfo?): String? = value?.getFullName()
}

private class StepikUserInfoFromString : StdConverter<String?, StepikUserInfo?>() {
  override fun convert(value: String?): StepikUserInfo? {
    if (value == null) {
      return null
    }
    return StepikUserInfo(value)
  }
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
abstract class LocalSectionMixin {
  @JsonProperty(TITLE)
  private lateinit var myName: String

  @JsonProperty(ITEMS)
  private lateinit var items: List<StudyItem>

  @JsonProperty(TYPE)
  fun getItemType(): String {
    throw NotImplementedInMixin()
  }
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
abstract class LocalLessonMixin {
  @JsonProperty(TITLE)
  private lateinit var myName: String

  @JsonProperty(TASK_LIST)
  private lateinit var items: List<StudyItem>

  @JsonProperty(TYPE)
  fun getItemType(): String {
    throw NotImplementedInMixin()
  }
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
abstract class FrameworkLessonMixin : LocalLessonMixin() {
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(IS_TEMPLATE_BASED)
  private var isTemplateBased: Boolean = true
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
abstract class LocalTaskMixin {
  @JsonProperty(NAME)
  private lateinit var myName: String

  @JsonProperty(FILES)
  private lateinit var myTaskFiles: MutableMap<String, TaskFile>

  @JsonProperty(DESCRIPTION_TEXT)
  private lateinit var descriptionText: String

  @JsonProperty(DESCRIPTION_FORMAT)
  private lateinit var descriptionFormat: DescriptionFormat

  @JsonProperty(FEEDBACK_LINK)
  private lateinit var myFeedbackLink: FeedbackLink

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var myCustomPresentableName: String? = null

  @JsonProperty(SOLUTION_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var solutionHidden: Boolean? = null

  @JsonProperty(TASK_TYPE)
  fun getItemType(): String {
    throw NotImplementedInMixin()
  }
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
abstract class ChoiceTaskLocalMixin : LocalTaskMixin() {

  @JsonProperty
  private var isMultipleChoice: Boolean = false

  @JsonProperty
  private lateinit var choiceOptions: List<ChoiceOption>
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
abstract class ChoiceOptionLocalMixin {
  @JsonProperty
  private var text: String = ""

  @JsonProperty
  private var status: ChoiceOptionStatus = ChoiceOptionStatus.UNKNOWN
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonPropertyOrder(NAME, PLACEHOLDERS, IS_VISIBLE, TEXT)
abstract class TaskFileMixin {
  @JsonProperty(NAME)
  private lateinit var myName: String

  @JsonProperty(PLACEHOLDERS)
  private lateinit var myAnswerPlaceholders: List<AnswerPlaceholder>

  @JsonProperty(IS_VISIBLE)
  var myVisible: Boolean = true

  @JsonProperty(TEXT)
  @Encrypt
  private lateinit var myText: String
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonPropertyOrder(OFFSET, LENGTH, DEPENDENCY, PLACEHOLDER_TEXT)
abstract class AnswerPlaceholderMixin {
  @JsonProperty(OFFSET)
  private var myOffset: Int = -1

  @JsonProperty(LENGTH)
  private var myLength: Int = -1

  @JsonProperty(DEPENDENCY)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private lateinit var myPlaceholderDependency: AnswerPlaceholderDependency

  @JsonProperty(PLACEHOLDER_TEXT)
  private lateinit var myPlaceholderText: String
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonPropertyOrder(OFFSET, LENGTH, DEPENDENCY, POSSIBLE_ANSWER, PLACEHOLDER_TEXT)
abstract class AnswerPlaceholderWithAnswerMixin : AnswerPlaceholderMixin() {
  @JsonProperty(POSSIBLE_ANSWER)
  @Encrypt
  private lateinit var myPossibleAnswer: String
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class AnswerPlaceholderDependencyMixin {
  @JsonProperty("section")
  private lateinit var mySectionName: String

  @JsonProperty("lesson")
  private lateinit var myLessonName: String

  @JsonProperty("task")
  private lateinit var myTaskName: String

  @JsonProperty("file")
  private lateinit var myFileName: String

  @JsonProperty("placeholder")
  private var myPlaceholderIndex: Int = -1

  @JsonProperty("is_visible")
  private var myIsVisible = true
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class FeedbackLinkMixin {
  @JsonProperty(LINK_TYPE)
  private lateinit var myType: FeedbackLink.LinkType

  @JsonProperty(LINK)
  private var myLink: String? = null
}

class VersionPropertyWriter : VirtualBeanPropertyWriter {

  @Suppress("unused")
  constructor()

  constructor(propDef: BeanPropertyDefinition, contextAnnotations: Annotations, declaredType: JavaType) : super(propDef,
                                                                                                                contextAnnotations,
                                                                                                                declaredType)

  override fun withConfig(config: MapperConfig<*>?,
                          declaringClass: AnnotatedClass,
                          propDef: BeanPropertyDefinition,
                          type: JavaType): VirtualBeanPropertyWriter {
    return VersionPropertyWriter(propDef, declaringClass.annotations, type)
  }

  override fun value(bean: Any, gen: JsonGenerator, prov: SerializerProvider): Any {
    return JSON_FORMAT_VERSION
  }
}

class CourseDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<Course>(vc) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): Course? {
    val node: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    return deserializeCourse(node, jp.codec)
  }

  private fun deserializeCourse(jsonObject: ObjectNode, codec: ObjectCodec): Course? {
    if (jsonObject.has(COURSE_TYPE)) {
      val courseType = jsonObject.get(COURSE_TYPE).asText()
      return when (courseType) {
        CourseraNames.COURSE_TYPE -> codec.treeToValue(jsonObject, CourseraCourse::class.java)
        else -> codec.treeToValue(jsonObject, EduCourse::class.java)
      }
    }
    return codec.treeToValue(jsonObject, EduCourse::class.java)
  }
}

class StudyItemDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<StudyItem>(vc) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): StudyItem? {
    val node: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    return deserializeItem(node, jp.codec)
  }

  private fun deserializeItem(jsonObject: ObjectNode, codec: ObjectCodec): StudyItem? {
    if (jsonObject.has(SerializationUtils.Json.TASK_TYPE)) {
      return doDeserializeTask(jsonObject, codec)
    }

    return if (!jsonObject.has(ITEM_TYPE)) {
      codec.treeToValue(jsonObject, Lesson::class.java)
    }
    else {
      val itemType = jsonObject.get(ITEM_TYPE).asText()
      when (itemType) {
        EduNames.LESSON -> codec.treeToValue(jsonObject, Lesson::class.java)
        FRAMEWORK_TYPE -> codec.treeToValue(jsonObject, FrameworkLesson::class.java)
        EduNames.SECTION -> codec.treeToValue(jsonObject, Section::class.java)
        else -> throw IllegalArgumentException("Unsupported item type: $itemType")
      }
    }
  }
}