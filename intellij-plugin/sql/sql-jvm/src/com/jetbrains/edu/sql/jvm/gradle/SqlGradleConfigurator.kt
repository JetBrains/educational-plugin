package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.core.SqlConfiguratorBase

class SqlGradleConfigurator : GradleConfiguratorBase(), SqlConfiguratorBase<JdkProjectSettings> {

  // A proper test template is provided via `SqlGradleCourseBuilder.testTemplateName`
  override val testFileName: String = ""

  override val courseBuilder: GradleCourseBuilderBase
    get() = SqlGradleCourseBuilder()

  override fun excludeFromArchive(holder: CourseInfoHolder<out Course?>, file: VirtualFile): Boolean {
    if (super<GradleConfiguratorBase>.excludeFromArchive(holder, file)) return true
    return file.extension == DB_EXTENSION
  }

  override val taskCheckerProvider: TaskCheckerProvider
    get() = GradleTaskCheckerProvider()

  companion object {
    private const val DB_EXTENSION = "db"
  }
}
