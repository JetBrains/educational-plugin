package com.jetbrains.edu.coursecreator

import com.jetbrains.edu.learning.courseFormat.tasks.Task

enum class FileSetKind(val fileSetName: String) {
  TASK_FILES("task files"),
  TEST_FILES("test files"),
  ADDITIONAL_FILES("additional files"),
  ALL_FILES("files");

  fun fileSet(task: Task): Set<String> = when (this) {
    TASK_FILES -> task.taskFiles.keys
    TEST_FILES -> task.testsText.keys
    ADDITIONAL_FILES -> task.additionalFiles.keys
    ALL_FILES -> task.taskFiles.keys + task.testsText.keys + task.additionalFiles.keys
  }
}

data class FileCheck(
  val task: Task,
  val path: String,
  val kind: FileSetKind,
  val shouldContain: Boolean
) {
  fun invert(): FileCheck = copy(shouldContain = !shouldContain)
  fun check() {
    if (shouldContain) {
      check(path in kind.fileSet(task)) {
        "`$path` should be in ${kind.fileSetName} of `${task.name}` task"
      }
    } else {
      check(path !in kind.fileSet(task)) {
        "`$path` shouldn't be in ${kind.fileSetName} of `${task.name}` task"
      }
    }
  }
}

infix fun Pair<String, FileSetKind>.`in`(task: Task): FileCheck = FileCheck(task, first, second, true)
infix fun Pair<String, FileSetKind>.notIn(task: Task): FileCheck = FileCheck(task, first, second, false)

infix fun String.`in`(task: Task): FileCheck = FileCheck(task, this, FileSetKind.ALL_FILES, true)
infix fun String.notIn(task: Task): FileCheck = FileCheck(task, this, FileSetKind.ALL_FILES, false)
