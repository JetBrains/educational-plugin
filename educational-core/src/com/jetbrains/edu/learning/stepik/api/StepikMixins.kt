package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.*
import java.util.*

const val IS_IDEA_COMPATIBLE = "is_idea_compatible"
const val COURSE_FORMAT = "course_format"
const val SECTIONS = "sections"
const val INSTRUCTORS = "instructors"
const val ID = "id"
const val UPDATE_DATE = "update_date"
const val IS_PUBLIC = "is_public"
const val SUMMARY = "summary"
const val TITLE = "title"
const val PROGRAMMING_LANGUAGE = "programming_language"
const val LANGUAGE = "language"
const val ADMINS_GROUP = "admins_group"
const val UNITS = "units"
const val COURSE = "course"
const val POSITION = "position"
const val STEPS = "steps"
const val UNIT_ID = "unit_id"
const val NAME = "name"
const val PLACEHOLDERS = "placeholders"
const val IS_VISIBLE = "is_visible"
const val TEXT = "text"
const val OFFSET = "offset"
const val LENGTH = "length"
const val DEPENDENCY = "dependency"
const val HINTS = "hints"
const val POSSIBLE_ANSWER = "possible_answer"
const val PLACEHOLDER_TEXT = "placeholder_text"
const val SELECTED = "selected"
const val SECTION = "section"
const val LESSON = "lesson"
const val TASK = "task"
const val FILE = "file"
const val PLACEHOLDER = "placeholder"
const val LINK_TYPE = "link_type"
const val LINK = "link"
const val STATUS = "status"
const val STEPIK_ID = "stepic_id"
const val FILES = "files"
const val CHOICE_VARIANTS = "choice_variants"
const val IS_MULTICHOICE = "is_multichoice"
const val SELECTED_VARIANTS = "selected_variants"

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
abstract class StepikEduCourseMixin {
  @JsonProperty(IS_IDEA_COMPATIBLE)
  var isCompatible = true

  @JsonProperty(COURSE_FORMAT)
  lateinit var myType: String

  @JsonProperty(SECTIONS)
  lateinit var sectionIds: List<Int>

  @JsonProperty(INSTRUCTORS)
  lateinit var instructors: List<Int>

  @JsonProperty(ID)
  private var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  lateinit var myUpdateDate: Date

  @JsonProperty(IS_PUBLIC)
  var isPublic: Boolean = false

  @JsonProperty(SUMMARY)
  lateinit var description: String

  @JsonProperty(TITLE)
  lateinit var name: String

  @JsonProperty(PROGRAMMING_LANGUAGE)
  lateinit var myProgrammingLanguage: String

  @JsonProperty(LANGUAGE)
  lateinit var myLanguageCode: String

  @JsonProperty(ADMINS_GROUP)
  lateinit var myAdminsGroup: String
}

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
class StepikSectionMixin {
  @JsonProperty(UNITS)
  lateinit var units: List<Int>

  @JsonProperty(COURSE)
  var courseId: Int = 0

  @JsonProperty(TITLE)
  lateinit var name: String

  @JsonProperty(POSITION)
  var position: Int = 0

  @JsonProperty(ID)
  private var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  lateinit var myUpdateDate: Date
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
class StepikLessonMixin {
  @JsonProperty(ID)
  var myId: Int = 0

  @JsonProperty(STEPS)
  lateinit var steps: MutableList<Int>

  @JsonProperty(IS_PUBLIC)
  var is_public: Boolean = false

  @JsonProperty(UPDATE_DATE)
  lateinit var myUpdateDate: Date

  @JsonProperty(TITLE)
  lateinit var name: String

  @JsonProperty(UNIT_ID)
  var unitId: Int = 0
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
class StepikTaskFileMixin {
  @JsonProperty(NAME)
  lateinit var myName: String

  @JsonProperty(PLACEHOLDERS)
  lateinit var myAnswerPlaceholders: MutableList<AnswerPlaceholder>

  @JsonProperty(IS_VISIBLE)
  var myVisible = true

  @JsonProperty(TEXT)
  lateinit var myText : String
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
class StepikAnswerPlaceholderMixin {
  @JsonProperty(OFFSET)
  var myOffset = -1

  @JsonProperty(LENGTH)
  private var myLength = -1

  @JsonProperty(DEPENDENCY)
  lateinit var myPlaceholderDependency: AnswerPlaceholderDependency

  @JsonProperty(HINTS)
  lateinit var myHints: List<String>

  @JsonProperty(POSSIBLE_ANSWER)
  lateinit var myPossibleAnswer : String

  @JsonProperty(PLACEHOLDER_TEXT)
  lateinit var myPlaceholderText: String

  @JsonProperty(SELECTED)
  private var mySelected = false
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
class StepikAnswerPlaceholderDependencyMixin {

  @JsonProperty(SECTION)
  lateinit var mySectionName: String

  @JsonProperty(LESSON)
  lateinit var myLessonName: String

  @JsonProperty(TASK)
  lateinit var myTaskName: String

  @JsonProperty(FILE)
  lateinit var myFileName: String

  @JsonProperty(PLACEHOLDER)
  var myPlaceholderIndex: Int = 0

  @JsonProperty(IS_VISIBLE)
  var myIsVisible = true
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
class StepikFeedbackLinkMixin {
  @JsonProperty(LINK_TYPE)
  lateinit var myType: FeedbackLink.LinkType

  @JsonProperty(LINK)
  lateinit var myLink: String
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
open class StepikTaskMixin {
  @JsonProperty(NAME)
  var name: String? = null

  @JsonProperty(STATUS)
  var myStatus = CheckStatus.Unchecked

  @JsonProperty(STEPIK_ID)
  private var myStepId: Int = 0

  @JsonProperty(FILES)
  private var myTaskFiles: MutableMap<String, TaskFile>? = LinkedHashMap()
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
class StepikChoiceTaskMixin : StepikTaskMixin() {
  @JsonProperty(CHOICE_VARIANTS)
  lateinit var myChoiceVariants: List<String>

  @JsonProperty(IS_MULTICHOICE)
  var myIsMultipleChoice: Boolean = false

  @JsonProperty(SELECTED_VARIANTS)
  lateinit var mySelectedVariants: List<Int>

}
