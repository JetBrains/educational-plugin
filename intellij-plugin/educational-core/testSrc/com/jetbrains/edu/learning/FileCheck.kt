package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

sealed interface FileCheck {
  fun invert(): FileCheck
  fun check()
}

data class TaskFileCheck(
  val task: Task,
  val path: String,
  val shouldContain: Boolean,
  val additionalCheck: ((TaskFile) -> Unit)? = null
) : FileCheck {
  override fun invert(): TaskFileCheck = copy(shouldContain = !shouldContain)
  override fun check() {
    val taskFile = task.taskFiles[path]
    if (shouldContain) {
      check(taskFile != null) {
        "`$path` should be in `${task.name}` task"
      }
      additionalCheck?.invoke(taskFile) // !! is safe because of `check` call
    } else {
      check(taskFile == null) {
        "`$path` shouldn't be in `${task.name}` task"
      }
    }
  }

  fun withAdditionalCheck(check: (TaskFile) -> Unit): TaskFileCheck = copy(additionalCheck = check)
}

infix fun String.`in`(task: Task): TaskFileCheck = TaskFileCheck(task, this, true)
infix fun String.notIn(task: Task): TaskFileCheck = TaskFileCheck(task, this, false)

data class AdditionalFileCheck(
  val course: Course,
  val path: String,
  val shouldContain: Boolean,
  val additionalCheck: ((EduFile) -> Unit)? = null
) : FileCheck {
  override fun invert(): AdditionalFileCheck = copy(shouldContain = !shouldContain)
  override fun check() {
    val additionalFile = course.additionalFiles.find { it.name == path }
    if (shouldContain) {
      check(additionalFile != null) {
        "`$path` should be in `${course.name}` course"
      }
      additionalCheck?.invoke(additionalFile) // !! is safe because of `check` call
    } else {
      check(additionalFile == null) {
        "`$path` shouldn't be in `${course.name}` course"
      }
    }
  }
}

infix fun String.`in`(course: Course): AdditionalFileCheck = AdditionalFileCheck(course, this, true)
infix fun String.notIn(course: Course): AdditionalFileCheck = AdditionalFileCheck(course, this, false)
