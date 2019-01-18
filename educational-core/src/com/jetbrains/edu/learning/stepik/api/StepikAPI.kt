@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.api

import com.google.gson.GsonBuilder
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikSteps
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
  lateinit var steps: List<StepikSteps.StepSource>
}

class SubmissionsList {
  lateinit var submissions: List<StepikWrappers.Submission>
}

class ProgressesList {
  lateinit var progresses: List<Progress>
}

class AttemptsList {
  lateinit var attempts: List<StepikWrappers.Attempt>
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
  var attempt: StepikWrappers.Attempt = StepikWrappers.Attempt(step)
}

class ViewData(assignment: Int, step: Int) {
  var view: View = View(assignment, step)
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
