@file:JvmName("LocalEduCourseMixins")
@file:Suppress("unused")

package com.jetbrains.edu.coursecreator.actions.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.AUTHORS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.CHOICE_OPTIONS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.CUSTOM_NAME
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.DEPENDENCY
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.DESCRIPTION_FORMAT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.DESCRIPTION_TEXT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ENVIRONMENT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.FILE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.FILES
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.IS_EDITABLE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.IS_MULTIPLE_CHOICE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.IS_VISIBLE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ITEMS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.LANGUAGE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.LENGTH
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.LESSON
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.MAX_VERSION
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.MESSAGE_CORRECT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.MESSAGE_INCORRECT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.MIN_VERSION
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.NAME
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.OFFSET
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLACEHOLDER
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLACEHOLDERS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLACEHOLDER_TEXT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLUGINS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLUGIN_ID
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLUGIN_NAME
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.POSSIBLE_ANSWER
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.QUIZ_HEADER
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SECTION
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SOLUTION_HIDDEN
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SUBMIT_MANUALLY
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SUMMARY
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TAGS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TASK
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TASK_LIST
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TASK_TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TEXT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TITLE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.VERSION
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.encrypt.Encrypt
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.FRAMEWORK_TYPE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.ITEM_TYPE
import com.jetbrains.edu.learning.serialization.TrueValueFilter
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.api.doDeserializeTask
import com.jetbrains.edu.learning.yaml.format.FeedbackCorrectFilter
import com.jetbrains.edu.learning.yaml.format.FeedbackIncorrectFilter
import com.jetbrains.edu.learning.yaml.format.NotImplementedInMixin
import com.jetbrains.edu.learning.yaml.format.QuizHeaderFilter


@JsonPropertyOrder(ENVIRONMENT, SUMMARY, TITLE, PROGRAMMING_LANGUAGE, LANGUAGE, COURSE_TYPE, SOLUTIONS_HIDDEN, PLUGINS,
                   ITEMS, AUTHORS, TAGS, ADDITIONAL_FILES, VERSION)
abstract class LocalEduCourseMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(AUTHORS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonSerialize(contentConverter = UserInfoToString::class)
  @JsonDeserialize(contentConverter = StepikUserInfoFromString::class)
  private var authors: MutableList<UserInfo> = ArrayList()

  @JsonProperty(SUMMARY)
  private lateinit var description: String

  @JsonProperty(PROGRAMMING_LANGUAGE)
  private lateinit var programmingLanguage: String

  @JsonProperty(LANGUAGE)
  private lateinit var languageCode: String

  @JsonProperty(ENVIRONMENT)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  lateinit var environment: String

  val itemType: String
    @JsonProperty(COURSE_TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(ITEMS)
  private lateinit var _items: List<StudyItem>

  @JsonProperty(ADDITIONAL_FILES)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var additionalFiles: List<TaskFile>

  @JsonProperty(PLUGINS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var pluginDependencies: List<PluginInfo>

  @JsonProperty(SOLUTIONS_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var solutionsHidden: Boolean = false

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>

  @JsonProperty(VERSION)
  private var formatVersion = JSON_FORMAT_VERSION
}

@JsonAutoDetect(setterVisibility = JsonAutoDetect.Visibility.NONE)
abstract class CourseraCourseMixin : LocalEduCourseMixin() {
  @JsonProperty(SUBMIT_MANUALLY)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var submitManually = false
}

private class UserInfoToString : StdConverter<UserInfo, String?>() {
  override fun convert(value: UserInfo?): String? = value?.getFullName()
}

private class StepikUserInfoFromString : StdConverter<String?, StepikUserInfo?>() {
  override fun convert(value: String?): StepikUserInfo? {
    if (value == null) {
      return null
    }
    return StepikUserInfo(value)
  }
}

abstract class PluginInfoMixin : PluginInfo() {
  @JsonProperty(PLUGIN_ID)
  override var stringId: String = ""

  @JsonProperty(PLUGIN_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  override var displayName: String? = null

  @JsonProperty(MIN_VERSION)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  override var minVersion: String? = null

  @JsonProperty(MAX_VERSION)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  override var maxVersion: String? = null
}

@JsonPropertyOrder(TITLE, CUSTOM_NAME, TAGS, ITEMS, TYPE)
abstract class LocalSectionMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(ITEMS)
  private lateinit var _items: List<StudyItem>

  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPropertyOrder(TITLE, CUSTOM_NAME, TAGS, TASK_LIST, TYPE)
abstract class LocalLessonMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(TASK_LIST)
  private lateinit var _items: List<StudyItem>

  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPropertyOrder(NAME, CUSTOM_NAME, TAGS, FILES, DESCRIPTION_TEXT, DESCRIPTION_FORMAT, FEEDBACK_LINK, SOLUTION_HIDDEN,
                   TASK_TYPE)
abstract class LocalTaskMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  @JsonProperty(FILES)
  private lateinit var _taskFiles: MutableMap<String, TaskFile>

  @JsonProperty(DESCRIPTION_TEXT)
  private lateinit var descriptionText: String

  @JsonProperty(DESCRIPTION_FORMAT)
  private lateinit var descriptionFormat: DescriptionFormat

  @JsonProperty(FEEDBACK_LINK)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private lateinit var feedbackLink: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(SOLUTION_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var solutionHidden: Boolean? = null

  val itemType: String
    @JsonProperty(TASK_TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPropertyOrder(CHOICE_OPTIONS, IS_MULTIPLE_CHOICE, MESSAGE_CORRECT, MESSAGE_INCORRECT, QUIZ_HEADER,
                   NAME, CUSTOM_NAME, TAGS, FILES, DESCRIPTION_TEXT, DESCRIPTION_FORMAT, FEEDBACK_LINK, SOLUTION_HIDDEN, TASK_TYPE)
abstract class ChoiceTaskLocalMixin : LocalTaskMixin() {

  @JsonProperty
  private var isMultipleChoice: Boolean = false

  @JsonProperty
  private lateinit var choiceOptions: List<ChoiceOption>

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FeedbackCorrectFilter::class)
  @JsonProperty
  private lateinit var messageCorrect: String

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FeedbackIncorrectFilter::class)
  @JsonProperty
  private lateinit var messageIncorrect: String

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = QuizHeaderFilter::class)
  @JsonProperty
  private lateinit var quizHeader: String
}

abstract class ChoiceOptionLocalMixin {
  @JsonProperty
  private var text: String = ""

  @JsonProperty
  private var status: ChoiceOptionStatus = ChoiceOptionStatus.UNKNOWN
}

@JsonPropertyOrder(NAME, PLACEHOLDERS, IS_VISIBLE, TEXT, IS_EDITABLE)
abstract class TaskFileMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  @JsonProperty(PLACEHOLDERS)
  private lateinit var _answerPlaceholders: List<AnswerPlaceholder>

  @JsonProperty(IS_VISIBLE)
  var isVisible: Boolean = true

  @JsonProperty(TEXT)
  @Encrypt
  private lateinit var text: String

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(IS_EDITABLE)
  var isEditable: Boolean = true
}

@JsonPropertyOrder(OFFSET, LENGTH, DEPENDENCY, PLACEHOLDER_TEXT)
abstract class AnswerPlaceholderMixin {
  @JsonProperty(OFFSET)
  private var offset: Int = -1

  @JsonProperty(LENGTH)
  private var length: Int = -1

  @JsonProperty(DEPENDENCY)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private lateinit var placeholderDependency: AnswerPlaceholderDependency

  @JsonProperty(PLACEHOLDER_TEXT)
  private lateinit var placeholderText: String
}

@JsonPropertyOrder(OFFSET, LENGTH, DEPENDENCY, POSSIBLE_ANSWER, PLACEHOLDER_TEXT)
abstract class AnswerPlaceholderWithAnswerMixin : AnswerPlaceholderMixin() {
  @JsonProperty(POSSIBLE_ANSWER)
  @Encrypt
  private lateinit var possibleAnswer: String
}

@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class AnswerPlaceholderDependencyMixin {
  @JsonProperty(SECTION)
  private lateinit var sectionName: String

  @JsonProperty(LESSON)
  private lateinit var lessonName: String

  @JsonProperty(TASK)
  private lateinit var taskName: String

  @JsonProperty(FILE)
  private lateinit var fileName: String

  @JsonProperty(PLACEHOLDER)
  private var placeholderIndex: Int = -1

  @JsonProperty(IS_VISIBLE)
  private var isVisible = true
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
        else -> {
          val course = codec.treeToValue(jsonObject, EduCourse::class.java)
          if (courseType == MARKETPLACE) {
            course.isMarketplace = true
          }
          course
        }
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
        LESSON -> codec.treeToValue(jsonObject, Lesson::class.java)
        FRAMEWORK_TYPE -> codec.treeToValue(jsonObject, FrameworkLesson::class.java)
        SECTION -> codec.treeToValue(jsonObject, Section::class.java)
        else -> throw IllegalArgumentException("Unsupported item type: $itemType")
      }
    }
  }
}