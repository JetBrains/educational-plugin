@file:Suppress("unused", "PropertyName")

package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.StepikWrappers
import com.jetbrains.edu.learning.stepik.serialization.StepikSubmissionTaskAdapter
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
  lateinit var units: List<StepikWrappers.Unit>
}

class StepsList {
  lateinit var steps: List<StepSource>
}

class StepSourcesList {
  @JsonProperty("step-sources")
  lateinit var steps: List<StepSource>
}

class SubmissionsList {
  lateinit var submissions: List<StepikWrappers.Submission>
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
  lateinit var submission: StepikWrappers.Submission

  constructor(attemptId: Int, score: String, files: ArrayList<StepikWrappers.SolutionFile>, task: Task) : this() {
    val serializedTask = GsonBuilder()   // TODO: use jackson
      .excludeFieldsWithoutExposeAnnotation()
      .registerTypeAdapter(Task::class.java, StepikSubmissionTaskAdapter())
      .create()
      .toJson(StepikWrappers.TaskWrapper(task))
    submission = StepikWrappers.Submission(score, attemptId, files, serializedTask)
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
  var unit: StepikWrappers.Unit = StepikWrappers.Unit()
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

class Member(var user: String, var group: String)

class MemberData(userId: String, group: String) {
  var member: Member = Member(userId, group)
}

// Auxiliary:

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