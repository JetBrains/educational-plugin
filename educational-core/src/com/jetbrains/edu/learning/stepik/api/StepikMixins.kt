@file:Suppress("unused", "PropertyName") // used for json serialization

package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NAME
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.submissions.*
import com.jetbrains.edu.learning.yaml.format.NotImplementedInMixin
import java.util.*

const val IS_IDEA_COMPATIBLE = "is_idea_compatible"
const val IS_ADAPTIVE = "is_adaptive"
const val COURSE_FORMAT = "course_format"
const val SECTIONS = "sections"
const val INSTRUCTORS = "instructors"
const val UPDATE_DATE = "update_date"
const val IS_PUBLIC = "is_public"
const val SUMMARY = "summary"
const val TITLE = "title"
const val PROGRAMMING_LANGUAGE = "programming_language"
const val LANGUAGE = "language"
const val ADMINS_GROUP = "admins_group"
const val lEARNERS_COUNT = "learners_count"
const val REVIEW_SUMMARY = "review_summary"
const val UNITS = "units"
const val COURSE = "course"
const val POSITION = "position"
const val STEPS = "steps"
const val NUMBER = "number"
const val OFFSET = "offset"
const val LENGTH = "length"
const val DEPENDENCY = "dependency"
const val POSSIBLE_ANSWER = "possible_answer"
const val PLACEHOLDER_TEXT = "placeholder_text"
const val SELECTED = "selected"
const val SECTION = "section"
const val LESSON = "lesson"
const val TASK = "task"
private const val FILE = "file"
const val PLACEHOLDER = "placeholder"
const val LINK_TYPE = "link_type"
const val LINK = "link"
const val STEPIK_ID = "stepic_id"
const val FILES = "files"
const val CHOICE_VARIANTS = "choice_variants"
const val IS_MULTICHOICE = "is_multichoice"
const val SELECTED_VARIANTS = "selected_variants"
const val CUSTOM_NAME = "custom_name"
const val SOLUTION_HIDDEN = "solution_hidden"
private const val TASK_TYPE = "task_type"

open class StepikItemMixin {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  lateinit var updateDate: Date
}

abstract class StepikEduCourseMixin : StepikItemMixin() {
  @JsonProperty(IS_IDEA_COMPATIBLE)
  var isCompatible = true

  @JsonProperty(IS_ADAPTIVE)
  var isAdaptive = false

  @JsonProperty(COURSE_FORMAT)
  lateinit var type: String

  @JsonProperty(SECTIONS)
  lateinit var sectionIds: List<Int>

  @JsonProperty(INSTRUCTORS)
  lateinit var instructors: List<Int>

  @JsonProperty(IS_PUBLIC)
  var isStepikPublic: Boolean = false

  @JsonProperty(SUMMARY)
  lateinit var description: String

  @JsonProperty(TITLE)
  lateinit var name: String

  @JsonProperty(PROGRAMMING_LANGUAGE)
  lateinit var programmingLanguage: String

  @JsonProperty(LANGUAGE)
  lateinit var languageCode: String

  @JsonProperty(ADMINS_GROUP)
  lateinit var adminsGroup: String

  @JsonProperty(lEARNERS_COUNT)
  var learnersCount: Int = 0

  @JsonProperty(REVIEW_SUMMARY)
  var reviewSummary: Int = 0
}

class StepikLessonMixin : StepikItemMixin() {
  @JsonProperty(STEPS)
  lateinit var stepIds: MutableList<Int>

  @JsonProperty(TITLE)
  lateinit var name: String

}

class StepikTaskFileMixin {
  @JsonProperty(NAME)
  lateinit var myName: String

  @JsonProperty(PLACEHOLDERS)
  lateinit var myAnswerPlaceholders: MutableList<AnswerPlaceholder>

  @JsonProperty(IS_VISIBLE)
  var myVisible = true

  @JsonProperty(TEXT)
  @JsonDeserialize(using = TaskFileTextDeserializer::class)
  lateinit var myText : String
}

class StepikAnswerPlaceholderMixin {
  @JsonProperty(OFFSET)
  var offset = -1

  @JsonProperty(LENGTH)
  private var length = -1

  @JsonProperty(DEPENDENCY)
  lateinit var placeholderDependency: AnswerPlaceholderDependency

  @JsonProperty(POSSIBLE_ANSWER)
  lateinit var possibleAnswer : String

  @JsonProperty(PLACEHOLDER_TEXT)
  lateinit var placeholderText: String

  @JsonProperty(SELECTED)
  private var selected = false
}

@JsonPropertyOrder(NAME, STEPIK_ID, STATUS, FILES, TASK_TYPE)
open class StepikTaskMixin {
  @JsonProperty(NAME)
  var name: String? = null

  @JsonProperty(STATUS)
  var checkStatus = CheckStatus.Unchecked

  @JsonProperty(STEPIK_ID)
  private var id: Int = 0

  @JsonProperty(FILES)
  private var _taskFiles: MutableMap<String, TaskFile> = LinkedHashMap()

  val itemType: String
    @JsonProperty(TASK_TYPE)
    get() = throw NotImplementedInMixin()
}

class TaskFileTextDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<String>(vc) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): String {
    return StringUtil.convertLineSeparators(jp.valueAsString)
  }
}
