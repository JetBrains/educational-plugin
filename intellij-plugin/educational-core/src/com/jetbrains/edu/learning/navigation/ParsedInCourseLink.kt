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

sealed class ParsedInCourseLink(val file: VirtualFile) {

  class ItemContainerDirectory(dir: VirtualFile) : ParsedInCourseLink(dir)
  class TaskDirectory(val task: Task, file: VirtualFile) : ParsedInCourseLink(file)
  class FileInTask(val task: Task, file: VirtualFile) : ParsedInCourseLink(file)

  companion object {

    fun parse(project: Project, rawLink: String): ParsedInCourseLink? {
      val course = project.course ?: return null
      return parseNextItem(project, course, rawLink)
    }

    private fun parseNextItem(project: Project, container: StudyItem, remainingPath: String?): ParsedInCourseLink? {
      if (remainingPath == null) {
        val courseDir = project.courseDir
        val dir = container.getDir(courseDir) ?: return null

        return when (container) {
          is ItemContainer -> ItemContainerDirectory(dir)
          is Task -> TaskDirectory(container, dir)
          else -> error("Unexpected item type: ${container.itemType}")
        }
      }

      return when (container) {
        is Task -> {
          val taskFile = container.getTaskFile(remainingPath) ?: return null
          val file = taskFile.getVirtualFile(project) ?: return null
          FileInTask(container, file)
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
