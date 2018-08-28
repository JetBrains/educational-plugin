package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.*

class FrameworkLesson() : Lesson() {

  constructor(lesson: Lesson): this() {
    remoteInfo = lesson.remoteInfo
    name = lesson.name
    taskList = lesson.taskList
    section = lesson.section
    index = lesson.index
    customPresentableName = lesson.customPresentableName
  }

  var currentTaskIndex: Int = 0

  /**
   * Contains diffs between neighbor tasks.
   * [diffs]`[i]` is diff list between [taskList]`[i]` and [taskList]`[i - 1]`.
   * [diffs]`[0]` is supposed to be empty.
   */
  @Transient
  private var diffs: List<List<TaskDiff>> = emptyList()

  fun currentTask(): Task = taskList[currentTaskIndex]

  override fun init(course: Course?, section: StudyItem?, isRestarted: Boolean) {
    super.init(course, section, isRestarted)
    // We don't need calculate diffs in CC mode because we don't use them
    if (course?.isStudy == true) {
      diffs = List(taskList.size) { index ->
        if (index == 0) return@List emptyList<TaskDiff>()
        val task = taskList[index]
        val prevTask = taskList[index - 1]
        calculateDiffs(prevTask, task)
      }
    }
  }

  fun prepareNextTask(project: Project, taskDir: VirtualFile) {
    check(EduUtils.isStudentProject(project)) {
      "`prepareNextTask` should be called only if course in study mode"
    }
    currentTaskIndex++
    diffs[currentTaskIndex].forEach { diff -> diff.apply(project, taskDir) }
  }

  fun preparePrevTask(project: Project, taskDir: VirtualFile) {
    check(EduUtils.isStudentProject(project)) {
      "`preparePrevTask` should be called only if course in study mode"
    }
    diffs[currentTaskIndex].forEach { diff -> diff.revert(project, taskDir) }
    currentTaskIndex--
  }

  private fun calculateDiffs(prevTask: Task, nextTask: Task): List<TaskDiff> {
    val diffs = mutableListOf<TaskDiff>()
    diffs += calculateDiffs(
      prevTask.taskFiles,
      nextTask.taskFiles,
      add = ::addTaskFile,
      remove = ::removeTaskFile,
      change = ::changeTaskFile
    )
    diffs += calculateDiffs(
      prevTask.testsText,
      nextTask.testsText,
      add = ::addFile,
      remove = ::removeFile,
      change = ::changeFile
    )
    diffs += calculateDiffs(
      prevTask.additionalFiles.mapValues { (_, file) -> file.getText() },
      nextTask.additionalFiles.mapValues { (_, file) -> file.getText() },
      add = ::addFile,
      remove = ::removeFile,
      change = ::changeFile
    )
    return diffs
  }
}

private inline fun <T> calculateDiffs(
  prevItems: Map<String, T>,
  nextItems: Map<String, T>,
  add: (String, T) -> TaskDiff,
  remove: (String, T) -> TaskDiff,
  change: (String, T, T) -> TaskDiff
): List<TaskDiff> {
  val allItems = prevItems.keys + nextItems.keys
  return allItems.mapNotNull { path ->
    val prevItem = prevItems[path]
    val nextItem = nextItems[path]
    when {
      prevItem == null && nextItem != null -> add(path, nextItem)
      prevItem != null && nextItem == null -> remove(path, prevItem)
      // TODO: implement `equals` for `TaskFile`
      prevItem != null && nextItem != null && prevItem != nextItem -> change(path, prevItem, nextItem)
      else -> null
    }
  }
}
