package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

open class HyperskillCourseProjectGenerator<T : Any>(
  private val base: CourseProjectGenerator<T>,
  builder: HyperskillCourseBuilder<T>,
  course: HyperskillCourse
) : CourseProjectGenerator<T>(builder, course) {
  override fun beforeProjectGenerated(): Boolean {
    return true
  }

  override fun afterProjectGenerated(project: Project, projectSettings: T) = base.afterProjectGenerated(project, projectSettings)

  override fun createAdditionalFiles(project: Project, holder: CourseInfoHolder<Course>, isNewCourse: Boolean) =
    base.createAdditionalFiles(project, holder, isNewCourse)

  override fun createCourseStructure(project: Project, module: Module, baseDir: VirtualFile, settings: T) =
    base.createCourseStructure(project, module, baseDir, settings)
}
