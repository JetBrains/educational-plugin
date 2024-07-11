@file:Suppress("unused", "PropertyName", "MemberVisibilityCanBePrivate")

package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ATTEMPT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CHECK_PROFILE
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NAME
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.attempts.AttemptBase
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TEXT
import com.jetbrains.edu.learning.stepik.ChoiceStepSource
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.hyperskill.api.WithPaginationMetaData
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOLUTIONS_HIDDEN
import java.util.*

const val USERS = "users"
const val META = "meta"
const val COURSES = "courses"
const val LESSONS = "lessons"
const val STEP_SOURCES = "step-sources"
const val SUBMISSIONS = "submissions"
const val PROGRESSES = "progresses"
const val ATTEMPTS = "attempts"
const val ASSIGNMENTS = "assignments"
const val ENROLLMENT = "enrollment"
const val VIEW = "view"
const val UNIT = "unit"
const val STEP_SOURCE = "step_source"
const val MEMBER = "member"
const val USER = "user"
const val GROUP = "group"
const val ASSIGNMENT = "assignment"
const val STEP = "step"
const val IS_PASSED = "is_passed"
const val IS_MULTIPLE_CHOICE = "is_multiple_choice"
const val PAIRS = "pairs"
const val OPTIONS = "options"
const val ORDERING = "ordering"
const val COLUMNS = "columns"
const val NAME_ROW = "name_row"
const val ANSWER = "answer"
const val DATASET = "dataset"
const val REPLY = "reply"
const val HINT = "hint"
const val FEEDBACK = "feedback"
const val MESSAGE = "message"
const val CHOICES = "choices"
const val SCORE = "score"
const val SOLUTION = "solution"
const val CODE = "code"
const val FILE = "file"
const val EDU_TASK = "edu"
const val CODE_TASK = "code"
const val REMOTE_EDU_TASK = "remote_edu"
const val NUMBER_TASK = "number"
const val STRING_TASK = "string"
const val SORTING_BASED_TASK = "sorting_based"
const val CHOICE_TASK = "choice"
const val DATA_TASK = "data"
const val TABLE_TASK = "table"
const val VERSION = "version"
const val ATTACHMENTS = "attachments"
const val COURSE_REVIEW_SUMMARIES = "course-review-summaries"
const val ADDITIONAL_FILES = "additional_files"
const val TASK_FILES = "task_files"
const val TASKS_INFO = "tasks_info"
const val MEMORY = "memory"
const val AVERAGE = "average"
const val FIRST_NAME = "first_name"
const val LAST_NAME = "last_name"
const val IS_GUEST = "is_guest"

// List wrappers for GET requests:

class UsersList {
  @JsonProperty(USERS)
  lateinit var users: List<StepikUserInfo>
}

class CoursesList {
  @JsonProperty(META)
  lateinit var meta: Map<Any, Any>

  @JsonProperty(COURSES)
  lateinit var courses: MutableList<EduCourse>
}

class SectionsList {
  @JsonProperty(SECTIONS)
  lateinit var sections: List<StepikSection>
}

class LessonsList {
  @JsonProperty(LESSONS)
  lateinit var lessons: List<StepikLesson>
}

class UnitsList {
  @JsonProperty(UNITS)
  lateinit var units: List<StepikUnit>
}

class StepsList {
  @JsonProperty(STEPS)
  lateinit var steps: List<StepSource>
}

class StepSourcesList {
  @JsonProperty(STEP_SOURCES)
  lateinit var steps: List<StepSource>
}

class ChoiceStepSourcesList {
  @JsonProperty(STEP_SOURCES)
  lateinit var steps: List<ChoiceStepSource>
}

class SubmissionsList : WithPaginationMetaData() {
  @JsonProperty(SUBMISSIONS)
  lateinit var submissions: List<StepikBasedSubmission>
}

class ProgressesList {
  @JsonProperty(PROGRESSES)
  lateinit var progresses: List<Progress>
}

class AttemptsList: WithPaginationMetaData() {
  @JsonProperty(ATTEMPTS)
  lateinit var attempts: List<Attempt>
}

class AssignmentsList {
  @JsonProperty(ASSIGNMENTS)
  lateinit var assignments: List<Assignment>
}

class AttachmentsList {
  @JsonProperty(ATTACHMENTS)
  lateinit var attachments: List<Attachment>
}

class CourseReviewSummariesList {
  @JsonProperty(COURSE_REVIEW_SUMMARIES)
  lateinit var courseReviewSummaries: List<CourseReviewSummary>
}

// Data wrappers for POST requests:

class EnrollmentData(courseId: Int) {
  @JsonProperty(ENROLLMENT)
  var enrollment: Enrollment = Enrollment(courseId.toString())
}

class AttemptData(step: Int) {
  @JsonProperty(ATTEMPT)
  var attempt: Attempt = Attempt(step)
}

class ViewData(assignment: Int, step: Int) {
  @JsonProperty(VIEW)
  var view: View = View(assignment, step)
}

class SectionData(@field:JsonProperty(SECTION) var section: Section)

class LessonData(lesson: Lesson) {
  @JsonProperty(LESSON)
  var lesson: StepikLesson = StepikLesson()

  init {
    this.lesson.name = lesson.name
    this.lesson.id = lesson.id
    this.lesson.stepIds = ArrayList()
  }
}

class UnitData(lessonId: Int, position: Int, sectionId: Int, unitId: Int? = null) {
  @JsonProperty(UNIT)
  var unit: StepikUnit = StepikUnit()

  init {
    unit.lesson = lessonId
    unit.position = position
    unit.section = sectionId
    unit.id = unitId
  }
}

class StepSourceData(project: Project, task: Task, lessonId: Int) {
  @JsonProperty(STEP_SOURCE)
  var stepSource: StepSource = StepSource(project, task, lessonId)
}

class MemberData(userId: String, group: String) {
  @JsonProperty(MEMBER)
  var member: Member = Member(userId, group)
}

class TaskData {
  @JsonProperty(TASK)
  lateinit var task: Task

  constructor()

  constructor(task: Task) {
    this.task = task
  }
}

// Auxiliary:

class StepikSection {
  @JsonProperty(UNITS)
  var units: List<Int> = listOf()

  @JsonProperty(TITLE)
  lateinit var name: String

  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  lateinit var updateDate: Date
}

class Member(@field:JsonProperty(USER) var user: String, @field:JsonProperty(GROUP) var group: String)

class Enrollment(@field:JsonProperty(COURSE) var course: String)

class View(@field:JsonProperty(ASSIGNMENT) var assignment: Int, @field:JsonProperty(STEP) var step: Int)

class Progress {
  @JsonProperty(ID)
  lateinit var id: String

  @JsonProperty(IS_PASSED)
  var isPassed: Boolean = false
}

class Assignment {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(STEP)
  var step: Int = 0
}

class StepikUnit {
  @JsonProperty(ID)
  var id: Int? = 0

  @JsonProperty(SECTION)
  var section: Int = 0

  @JsonProperty(LESSON)
  var lesson: Int = 0

  @JsonProperty(POSITION)
  var position: Int = 0

  @JsonProperty(ASSIGNMENTS)
  var assignments: List<Int> = mutableListOf()

  @JsonProperty(UPDATE_DATE)
  var updateDate: Date = Date()
}

class Feedback {
  @JsonProperty(MESSAGE)
  var message: String? = null

  constructor()

  constructor(feedback: String) {
    message = feedback
  }
}


open class Reply {
  @JsonProperty(VERSION)
  var version = JSON_FORMAT_VERSION
}

class CodeTaskReply: Reply() {
  @JsonProperty(LANGUAGE)
  var language: String? = null

  @JsonProperty(CODE)
  var code: String? = null
}

class EduTaskReply: Reply() {
  @JsonProperty(FEEDBACK)
  var feedback: Feedback? = null

  @JsonProperty(SCORE)
  var score: String = ""

  @JsonProperty(SOLUTION)
  var solution: List<SolutionFile>? = null

  @JsonProperty(CHECK_PROFILE)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var checkProfile: String? = null
}

class ChoiceTaskReply: Reply() {
  @JsonProperty(CHOICES)
  var choices: BooleanArray? = null
}

class SortingBasedTaskReply: Reply() {
  @JsonProperty(ORDERING)
  var ordering: IntArray? = null
}

class DataTaskReply: Reply() {
  @JsonProperty(FILE)
  var file: String? = null
}

class NumberTaskReply: Reply() {
  @JsonProperty(NUMBER)
  var number: String? = null
}

class TextTaskReply: Reply() {
  @JsonProperty(TEXT)
  var text: String? = null
}

class TableTaskReply: Reply() {
  @JsonProperty(CHOICES)
  var choices: Array<Row>? = null
}

class Row {
  @JsonProperty(NAME_ROW)
  var nameRow: String? = null

  @JsonProperty(COLUMNS)
  var columns: Array<Column>? = null
}

class Column {
  @JsonProperty(NAME)
  var name: String? = null

  @JsonProperty(ANSWER)
  var answer: Boolean? = null
}

class Attachment {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(NAME)
  lateinit var name: String
}

class CourseReviewSummary {
  @JsonProperty(AVERAGE)
  var average: Double = 0.0

  @JsonProperty(COURSE)
  var courseId: Int = 0
}

open class AdditionalInfo

class CourseAdditionalInfo : AdditionalInfo {
  @JsonProperty(ADDITIONAL_FILES)
  lateinit var additionalFiles: List<EduFile>

  @JsonProperty(SOLUTIONS_HIDDEN)
  var solutionsHidden: Boolean = false

  constructor()

  constructor(additionalFiles: List<EduFile>, solutionsHidden: Boolean = false) {
    this.additionalFiles = additionalFiles
    this.solutionsHidden = solutionsHidden
  }
}

class LessonAdditionalInfo : AdditionalInfo {
  // needed for com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.GetHyperskillLesson
  @JsonProperty(ADDITIONAL_FILES)
  var additionalFiles: List<EduFile> = listOf()

  @JsonProperty(CUSTOM_NAME)
  var customName: String? = null

  /**
   * We have another mechanism to store info about plugin tasks: com.jetbrains.edu.learning.stepik.PyCharmStepOptions
   * This object is used to store additional info about lesson or non-plugin tasks
   * (we use lessonInfo for tasks because Stepik API does not have attachments for tasks)
   * */

  @JsonProperty(TASKS_INFO)
  var tasksInfo: Map<Int, TaskAdditionalInfo> = emptyMap()

  constructor()

  constructor(customName: String?, tasksInfo: Map<Int, TaskAdditionalInfo>, additionalFiles: List<EduFile>) {
    this.customName = customName
    this.tasksInfo = tasksInfo
    this.additionalFiles = additionalFiles
  }

  val isEmpty: Boolean get() = customName.isNullOrEmpty() && tasksInfo.isEmpty() && additionalFiles.isEmpty()
}

// Not inherited from AdditionalInfo because Stepik does not support Attachments for tasks
class TaskAdditionalInfo {
  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(CUSTOM_NAME)
  var customName: String? = null

  @JsonProperty(TASK_FILES)
  lateinit var taskFiles: List<TaskFile>

  constructor()

  constructor(name: String, customName: String?, taskFiles: List<TaskFile>) {
    this.name = name
    this.customName = customName
    this.taskFiles = taskFiles
  }
}

class StepikBasedSubmission : Submission {
  @JsonProperty(ATTEMPT)
  var attempt: Int = 0

  @JsonProperty(REPLY)
  var reply: Reply? = null

  @JsonProperty(HINT)
  var hint: String? = null

  @JsonProperty(FEEDBACK)
  var feedback: Feedback? = null

  // WRITE_ONLY because we don't need to send it
  @JsonProperty(STEP, access = JsonProperty.Access.WRITE_ONLY)
  override var taskId: Int = -1

  private val LOG = logger<StepikBasedSubmission>()

  override val solutionFiles: List<SolutionFile>?
    get() {
      val submissionReply = reply
      // https://youtrack.jetbrains.com/issue/EDU-1449
      val solution = (submissionReply as? EduTaskReply)?.solution
      if (submissionReply != null && solution == null) {
        LOG.warn("`solution` field of reply object is null for task $taskId")
      }
      return solution
    }

  override val formatVersion: Int?
    get() = reply?.version

  override fun getSubmissionTexts(taskName: String): Map<String, String>? {
    return if (solutionFiles == null) {
      val submissionText = (reply as? CodeTaskReply)?.code ?: return null
      mapOf(taskName to submissionText)
    }
    else {
      super.getSubmissionTexts(taskName)
    }
  }

  constructor()

  constructor(attempt: AttemptBase, reply: Reply) {
    this.attempt = attempt.id
    this.reply = reply
  }
}
