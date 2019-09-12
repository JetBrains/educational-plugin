@file:JvmName("LocalEduCourseMixins")

package com.jetbrains.edu.coursecreator.actions.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.coursecreator.yaml.format.NotImplementedInMixin
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.FRAMEWORK_TYPE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.ITEM_TYPE
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.api.doDeserializeTask
import java.util.*

private const val VERSION = "version"
private const val TITLE = "title"
private const val AUTHORS = "authors"
private const val LANGUAGE = "language"
private const val SUMMARY = "summary"
private const val PROGRAMMING_LANGUAGE = "programming_language"
private const val ITEMS = "items"
private const val NAME = "name"
private const val TASK_LIST = "task_list"
private const val FILES = "files"
private const val TASK_TYPE = "task_type"
private const val DESCRIPTION_TEXT = "description_text"
private const val DESCRIPTION_FORMAT = "description_format"
private const val TEXT = "text"
private const val IS_VISIBLE = "is_visible"
private const val FEEDBACK_LINK = "feedback_link"
private const val PLACEHOLDERS = "placeholders"
private const val TYPE = "type"
private const val LINK = "link"
private const val LINK_TYPE = "link_type"
private const val OFFSET = "offset"
private const val LENGTH = "length"
private const val PLACEHOLDER_TEXT = "placeholder_text"
private const val POSSIBLE_ANSWER = "possible_answer"
private const val DEPENDENCY = "dependency"
private const val COURSE_TYPE = "course_type"
private const val ADDITIONAL_FILES = "additional_files"
private const val ENVIRONMENT = "environment"
private const val SUBMIT_MANUALLY = "submit_manually"
private const val CUSTOM_NAME = "custom_name"

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonPropertyOrder(VERSION, ENVIRONMENT, SUMMARY, TITLE, AUTHORS, PROGRAMMING_LANGUAGE, LANGUAGE, COURSE_TYPE,
                   YamlMixinNames.SOLUTIONS_HIDDEN, ITEMS)
@JsonSerialize(using = CourseSerializer::class)
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

  @JsonProperty(YamlMixinNames.SOLUTIONS_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var solutionsHidden: Boolean = false
}

@JsonAutoDetect(setterVisibility = JsonAutoDetect.Visibility.NONE)
abstract class CourseraCourseMixin : LocalEduCourseMixin() {
  @JsonProperty(SUBMIT_MANUALLY)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var submitManually = false
}

private class StepikUserInfoToString : StdConverter<StepikUserInfo, String?>() {
  override fun convert(value: StepikUserInfo?): String? = value?.name
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

class CourseSerializer : JsonSerializer<Course>() {
  override fun serialize(course: Course, generator: JsonGenerator, provider: SerializerProvider) {
    generator.writeStartObject()
    val serializer = getJsonSerializer(provider, course.javaClass)
    serializer.unwrappingSerializer(null).serialize(course, generator, provider)
    generator.writeObjectField(VERSION, JSON_FORMAT_VERSION)
    generator.writeEndObject()
  }
}

private fun getJsonSerializer(provider: SerializerProvider, itemClass: Class<out StudyItem>): JsonSerializer<Any> {
  val javaType = provider.constructType(itemClass)
  val beanDesc: BeanDescription = provider.config.introspect(javaType)
  return BeanSerializerFactory.instance.findBeanSerializer(provider, javaType, beanDesc)
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