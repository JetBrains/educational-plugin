package com.jetbrains.edu.learning.projectView

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator

data class CourseViewContext(
  private val project: Project,
  private val courseDir: VirtualFile,
  val course: Course,
  val configurator: EduConfigurator<*>,
  private val additionalFiles: Set<String>
) {

  fun containsAdditionalFile(file: VirtualFile): Boolean {
    val relativePath = VfsUtil.getRelativePath(courseDir, file) ?: return false
    return relativePath in additionalFiles
  }

  companion object {
    fun create(project: Project, course: Course): CourseViewContext? {
      val configurator = course.configurator ?: return null
      val additionalFiles = course.additionalFiles.map { it.name }.toSet()

      return CourseViewContext(
        project,
        project.courseDir,
        course,
        configurator,
        additionalFiles
      )
    }
  }
}
