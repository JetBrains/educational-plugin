package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun VirtualFile.fileInfo(project: Project): FileInfo? {
  if (project.isDisposed) return null
  val course = StudyTaskManager.getInstance(project).course ?: return null
  if (shouldIgnore(this, project)) return null

  if (isDirectory) {
    EduUtils.getSection(this, course)?.let { return FileInfo.SectionDirectory(it) }
    EduUtils.getLesson(this, course)?.let { return FileInfo.LessonDirectory(it) }
    EduUtils.getTask(this, course)?.let { return FileInfo.TaskDirectory(it) }
  }

  val task = EduUtils.getTaskForFile(project, this) ?: return null

  val taskRelativePath = EduUtils.pathRelativeToTask(project, this)

  if (taskRelativePath.contains(EduNames.WINDOW_POSTFIX)
      || taskRelativePath.contains(EduNames.WINDOWS_POSTFIX)
      || taskRelativePath.contains(EduNames.ANSWERS_POSTFIX)) {
    return null
  }

  return FileInfo.FileInTask(task, taskRelativePath)
}

private fun shouldIgnore(file: VirtualFile, project: Project): Boolean {
  val courseDir = EduUtils.getCourseDir(project)
  if (!FileUtil.isAncestor(courseDir.path, file.path, true)) return true
  val course = StudyTaskManager.getInstance(project).course ?: return true
  if (course.configurator?.excludeFromArchive(project, file) == true) return true
  return false
}

sealed class FileInfo {
  data class SectionDirectory(val section: Section) : FileInfo()
  data class LessonDirectory(val lesson: Lesson) : FileInfo()
  data class TaskDirectory(val task: Task) : FileInfo()
  data class FileInTask(val task: Task, val pathInTask: String) : FileInfo()
}
