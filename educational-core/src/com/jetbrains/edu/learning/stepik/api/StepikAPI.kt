@file:Suppress("unused", "PropertyName", "MemberVisibilityCanBePrivate")

package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.mixins.TaskSerializer
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import java.util.*

// List wrappers for GET requests:

class UsersList {
  lateinit var meta: Any
  lateinit var users: List<StepikUserInfo>
}

class CoursesList {
  lateinit var meta: Map<Any, Any>
  lateinit var courses: MutableList<EduCourse>
}

class SectionsList {
  lateinit var sections: List<Section>
}

class LessonsList {
  lateinit var lessons: List<Lesson>
}

class UnitsList {
  lateinit var units: List<StepikUnit>
}

class StepsList {
  lateinit var steps: List<StepSource>
}

class StepSourcesList {
  @JsonProperty("step-sources")
  lateinit var steps: List<StepSource>
}

class SubmissionsList {
  lateinit var submissions: List<Submission>
}

class ProgressesList {
  lateinit var progresses: List<Progress>
}

class AttemptsList {
  lateinit var attempts: List<Attempt>
}

class AssignmentsList {
  lateinit var assignments: List<Assignment>
}

// Data wrappers for POST requests:

class EnrollmentData(courseId: Int) {
  var enrollment: Enrollment = Enrollment(courseId.toString())
}

class SubmissionData() {
  lateinit var submission: Submission

  constructor(attemptId: Int, score: String, files: ArrayList<SolutionFile>, task: Task) : this() {
    val module = SimpleModule()
    module.addSerializer(Task::class.java, TaskSerializer())
    val objectMapper = StepikConnector.createMapper(module)
    val serializedTask = objectMapper.writeValueAsString(TaskData(task))

    submission = Submission(score, attemptId, files, serializedTask)
  }
}

class AttemptData(step: Int) {
  var attempt: Attempt = Attempt(step)
}

class ViewData(assignment: Int, step: Int) {
  var view: View = View(assignment, step)
}

class CourseData(course: Course) {
  var course: EduCourse = EduCourse()

  init {
    this.course.name = course.name
    this.course.language = course.language
    this.course.description = course.description
    this.course.authors = course.authors
    if (course is EduCourse && course.isRemote) {
      this.course.instructors = course.instructors
      this.course.isPublic = course.isPublic
    }
  }
}

class SectionData(var section: Section)

class LessonData(lesson: Lesson) {
  var lesson: Lesson = Lesson()

  init {
    this.lesson.name = lesson.name
    this.lesson.id = lesson.id
    this.lesson.steps = ArrayList()
    this.lesson.isPublic = true
  }
}

class UnitData(lessonId: Int, position: Int, sectionId: Int, unitId: Int? = null) {
  var unit: StepikUnit = StepikUnit()
  init {
    unit.lesson = lessonId
    unit.position = position
    unit.section = sectionId
    unit.id = unitId
  }
}

class StepSourceData(project: Project, task: Task, lessonId: Int) {
  var stepSource: StepSource = StepSource(project, task, lessonId)
}

class MemberData(userId: String, group: String) {
  var member: Member = Member(userId, group)
}

class TaskData(var task: Task)

// Auxiliary:

class Member(var user: String, var group: String)

class Enrollment(var course: String)

class View(var assignment: Int, var step: Int)

class Progress {
  lateinit var id: String
  var isPassed: Boolean = false
}

class Assignment {
  var id: Int = 0
  var step: Int = 0
}

class Dataset {
  constructor()
  constructor(emptyDataset: String)

  var is_multiple_choice: Boolean = false
  var options: List<String>? = null
}

class Attempt {
  constructor()

  constructor(step: Int) {
    this.step = step
  }

  var step: Int = 0
  var dataset: Dataset? = null
  var status: String? = null
  var user: String? = null
  var id: Int = 0

  val isActive: Boolean
    @JsonIgnore
    get() = status == "active"
}

class StepikUnit {
  var id: Int? = 0
  var section: Int = 0
  var lesson: Int = 0
  var position: Int = 0
  var assignments: List<Int>? = null
  @JsonProperty("update_date")
  var updateDate: Date = Date()
}


class Submission {
  var attempt: Int = 0
  var reply: Reply? = null
  var id: String? = null
  var status: String? = null
  var hint: String? = null

  constructor()

  constructor(score: String, attemptId: Int, files: ArrayList<SolutionFile>, serializedTask: String) {
    reply = Reply(files, score, serializedTask)
    this.attempt = attemptId
  }
}

class Reply {
  var choices: BooleanArray? = null
  var score: String? = null
  var solution: List<SolutionFile>? = null
  var language: String? = null
  var code: String? = null
  @JsonProperty("edu_task")
  var eduTask: String? = null
  var version = JSON_FORMAT_VERSION

  constructor()

  constructor(files: List<SolutionFile>, score: String, serializedTask: String) {
    this.score = score
    solution = files
    eduTask = serializedTask
  }
}

class SolutionFile {
  var name: String = ""
  var text: String = ""

  constructor()

  constructor(name: String, text: String) {
    this.name = name
    this.text = text
  }
}