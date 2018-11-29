package com.jetbrains.edu.coursecreator

import com.jetbrains.edu.learning.courseFormat.tasks.Task

data class FileCheck(
  val task: Task,
  val path: String,
  val shouldContain: Boolean
) {
  fun invert(): FileCheck = copy(shouldContain = !shouldContain)
  fun check() {
    if (shouldContain) {
      check(path in task.taskFiles.keys) {
        "`$path` should be in `${task.name}` task"
      }
    } else {
      check(path !in task.taskFiles.keys) {
        "`$path` shouldn't be in `${task.name}` task"
      }
    }
  }
}

infix fun String.`in`(task: Task): FileCheck = FileCheck(task, this, true)
infix fun String.notIn(task: Task): FileCheck = FileCheck(task, this, false)
