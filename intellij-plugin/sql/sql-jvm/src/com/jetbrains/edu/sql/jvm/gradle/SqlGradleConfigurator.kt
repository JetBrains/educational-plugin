package com.jetbrains.edu.sql.jvm.gradle

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.sql.core.SqlConfiguratorBase

class SqlGradleConfigurator : GradleConfiguratorBase(), SqlConfiguratorBase<JdkProjectSettings> {

  // A proper test template is provided via `SqlGradleCourseBuilder.testTemplateName`
  override val testFileName: String = ""

  override val courseBuilder: GradleCourseBuilderBase
    get() = SqlGradleCourseBuilder()

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super<GradleConfiguratorBase>.courseFileAttributesEvaluator) {
    extension(DB_EXTENSION) {
      @Suppress("DEPRECATION")
      legacyExcludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }
  }

  override val taskCheckerProvider: TaskCheckerProvider
    get() = GradleTaskCheckerProvider()

  companion object {
    private const val DB_EXTENSION = "db"
  }
}
