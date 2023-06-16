package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.core.SqlConfiguratorBase

abstract class SqlGradleConfiguratorBase : GradleConfiguratorBase(), SqlConfiguratorBase<JdkProjectSettings> {

  override fun excludeFromArchive(project: Project, course: Course, file: VirtualFile): Boolean {
    if (super<GradleConfiguratorBase>.excludeFromArchive(project, course, file)) return true
    return file.extension == DB_EXTENSION
  }

  override val taskCheckerProvider: TaskCheckerProvider
    get() = GradleTaskCheckerProvider()

  companion object {
    private const val DB_EXTENSION = "db"
  }
}
