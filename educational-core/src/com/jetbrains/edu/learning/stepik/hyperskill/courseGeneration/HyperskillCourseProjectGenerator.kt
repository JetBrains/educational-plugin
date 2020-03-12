package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

open class HyperskillCourseProjectGenerator<T>(private val base: CourseProjectGenerator<T>, builder: HyperskillCourseBuilder<T>,
                                               private val course: HyperskillCourse) : CourseProjectGenerator<T>(builder, course) {
  override fun beforeProjectGenerated(): Boolean {
    return true
  }

  override fun afterProjectGenerated(project: Project, projectSettings: T) = base.afterProjectGenerated(project, projectSettings)

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) = base.createAdditionalFiles(project, baseDir)

  override fun createCourseStructure(project: Project, module: Module, baseDir: VirtualFile, settings: T) =
    base.createCourseStructure(project, module, baseDir, settings)
}
