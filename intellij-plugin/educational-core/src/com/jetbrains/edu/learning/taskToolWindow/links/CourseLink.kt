package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.LinkType.IN_COURSE

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class CourseLink(link: String) : TaskDescriptionLink<ParsedInCourseLink, ParsedInCourseLink?>(link, IN_COURSE) {

  override fun resolve(project: Project): ParsedInCourseLink? {
    val course = project.course ?: return null
    return parseNextItem(project, course, linkPath)
  }

  private fun parseNextItem(project: Project, container: StudyItem, remainingPath: String?): ParsedInCourseLink? {
    if (remainingPath == null) {
      val courseDir = project.courseDir
      val dir = container.getDir(courseDir) ?: return null

      return when (container) {
        is ItemContainer -> ParsedInCourseLink.ItemContainerDirectory(dir)
        is Task -> ParsedInCourseLink.TaskDirectory(container, dir)
        else -> error("Unexpected item type: ${container.itemType}")
      }
    }

    return when (container) {
      is Task -> {
        val taskFile = container.getTaskFile(remainingPath) ?: return null
        val file = taskFile.getVirtualFile(project) ?: return null
        ParsedInCourseLink.FileInTask(container, file)
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

  override fun open(project: Project, parsedLink: ParsedInCourseLink) {
    runInEdt {
      parsedLink.navigate(project)
    }
  }

  override suspend fun validate(project: Project, parsedLink: ParsedInCourseLink?): String? {
    return if (parsedLink == null) "Failed to find an item in course by `$linkPath` path" else null
  }
}

sealed class ParsedInCourseLink(val file: VirtualFile) {

  abstract fun navigate(project: Project)

  class ItemContainerDirectory(dir: VirtualFile) : ParsedInCourseLink(dir) {
    override fun navigate(project: Project) {
      OpenFileDescriptor(project, file).navigate(true)
    }

  }

  class TaskDirectory(val task: Task, file: VirtualFile) : ParsedInCourseLink(file) {
    override fun navigate(project: Project) {
      NavigationUtils.navigateToTask(project, task, closeOpenedFiles = false)
    }
  }

  class FileInTask(val task: Task, file: VirtualFile) : ParsedInCourseLink(file) {
    override fun navigate(project: Project) {
      NavigationUtils.navigateToTask(project, task, closeOpenedFiles = false, fileToActivate = file)
    }
  }
}
