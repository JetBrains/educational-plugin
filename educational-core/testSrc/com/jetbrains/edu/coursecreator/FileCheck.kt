package com.jetbrains.edu.coursecreator

import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

data class FileCheck(
  val task: Task,
  val path: String,
  val shouldContain: Boolean,
  val additionalCheck: ((TaskFile) -> Unit)? = null
) {
  fun invert(): FileCheck = copy(shouldContain = !shouldContain)
  fun check() {
    val taskFile = task.taskFiles[path]
    if (shouldContain) {
      check(taskFile != null) {
        "`$path` should be in `${task.name}` task"
      }
      additionalCheck?.invoke(taskFile!!) // !! is safe because of `check` call
    } else {
      check(taskFile == null) {
        "`$path` shouldn't be in `${task.name}` task"
      }
    }
  }

  fun withAdditionalCheck(check: (TaskFile) -> Unit): FileCheck = copy(additionalCheck = check)
}

infix fun String.`in`(task: Task): FileCheck = FileCheck(task, this, true)
infix fun String.notIn(task: Task): FileCheck = FileCheck(task, this, false)
