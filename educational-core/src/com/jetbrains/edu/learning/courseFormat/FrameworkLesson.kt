package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.ext.testDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.*

class FrameworkLesson : Lesson() {

  @Transient
  var currentTaskIndex: Int = 0

  /**
   * Contains diffs between neighbor tasks.
   * [myDiffs]`[i]` is diff list between [taskList]`[i]` and [taskList]`[i - 1]`.
   * [myDiffs]`[0]` is supposed to be empty.
   */
  @Transient
  private var myDiffs: List<List<TaskDiff>>? = null

  override fun initLesson(course: Course?, isRestarted: Boolean) {
    super.initLesson(course, isRestarted)
    myDiffs = List(taskList.size) { index ->
      if (index == 0) return@List emptyList<TaskDiff>()
      val task = taskList[index]
      val prevTask = taskList[index - 1]
      calculateDiffs(prevTask, task)
    }
  }

  fun prepareNextTask(project: Project, taskDir: VirtualFile) {
    currentTaskIndex++
    myDiffs?.get(currentTaskIndex)?.forEach { diff -> diff.apply(project, taskDir) }
  }

  fun preparePrevTask(project: Project, taskDir: VirtualFile) {
    myDiffs?.get(currentTaskIndex)?.forEach { diff -> diff.revert(project, taskDir) }
    currentTaskIndex--
  }

  private fun calculateDiffs(prevTask: Task, nextTask: Task): List<TaskDiff> {
    val diffs = mutableListOf<TaskDiff>()
    diffs += calculateDiffs(
      prevTask.taskFiles,
      nextTask.taskFiles,
      course.sourceDir,
      add = ::addTaskFile,
      remove = ::removeTaskFile,
      change = ::changeTaskFile
    )
    diffs += calculateDiffs(
      prevTask.testsText,
      nextTask.testsText,
      course.testDir,
      add = ::addFile,
      remove = ::removeFile,
      change = ::changeFile
    )
    diffs += calculateDiffs(
      prevTask.additionalFiles,
      nextTask.additionalFiles,
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
  pathPrefix: String? = null,
  add: (String, T) -> TaskDiff,
  remove: (String, T) -> TaskDiff,
  change: (String, T, T) -> TaskDiff
): List<TaskDiff> {
  val allItems = prevItems.keys + nextItems.keys
  return allItems.mapNotNull { path ->
    val prevItem = prevItems[path]
    val nextItem = nextItems[path]
    val resultPath = if (pathPrefix.isNullOrEmpty()) path else "$pathPrefix/$path"
    when {
      prevItem == null && nextItem != null -> add(resultPath, nextItem)
      prevItem != null && nextItem == null -> remove(resultPath, prevItem)
      // TODO: implement `equals` for `TaskFile`
      prevItem != null && nextItem != null && prevItem != nextItem -> change(resultPath, prevItem, nextItem)
      else -> null
    }
  }
}
