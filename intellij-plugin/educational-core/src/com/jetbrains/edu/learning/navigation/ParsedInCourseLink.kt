package com.jetbrains.edu.learning.navigation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Structured form of a link to item in a course.
 */
sealed class ParsedInCourseLink<T : StudyItem>(val file: VirtualFile, val item: T) {

  class ItemContainerDirectory(dir: VirtualFile, container: ItemContainer) : ParsedInCourseLink<ItemContainer>(dir, container)
  class TaskDirectory(file: VirtualFile, task: Task) : ParsedInCourseLink<Task>(file, task)
  class FileInTask(file: VirtualFile, task: Task, val pathInTask: String) : ParsedInCourseLink<Task>(file, task)

  companion object {

    /**
     * Parses [rawLink] to item in a course.
     * [rawLink] is supposed to be in form of a relative path to the corresponding directory/file with OS-independent path separator
     *
     * @see [StudyItem.pathInCourse]
     */
    fun parse(project: Project, rawLink: String): ParsedInCourseLink<*>? {
      val course = project.course ?: return null
      return parseNextItem(project, course, rawLink)
    }

    private fun parseNextItem(project: Project, container: StudyItem, remainingPath: String?): ParsedInCourseLink<*>? {
      if (remainingPath == null) {
        val courseDir = project.courseDir
        val dir = container.getDir(courseDir) ?: return null

        return when (container) {
          is ItemContainer -> ItemContainerDirectory(dir, container)
          is Task -> TaskDirectory(dir, container)
          else -> error("Unexpected item type: ${container.itemType}")
        }
      }

      return when (container) {
        is Task -> {
          val taskFile = container.getTaskFile(remainingPath) ?: return null
          val file = taskFile.getVirtualFile(project) ?: return null
          FileInTask(file, container, remainingPath)
        }
        is ItemContainer -> {
          val segments = remainingPath.split("/", limit = 2)
          val childItemName = segments[0]
          val childItem = container.getItem(childItemName) ?: return null
          parseNextItem(project, childItem, segments.getOrNull(1))
        }
        else -> null
      }
    }
  }
}
