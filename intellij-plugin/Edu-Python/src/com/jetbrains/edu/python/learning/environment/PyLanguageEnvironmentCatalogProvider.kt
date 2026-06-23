package com.jetbrains.edu.python.learning.environment

import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.jetbrains.edu.learning.DefaultSettingsUtils.findPath
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.flatMap
import com.jetbrains.edu.learning.newproject.environment.EnvironmentUiKind
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalog
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider
import com.jetbrains.edu.python.learning.newproject.collectPyEnvironments
import com.jetbrains.edu.python.learning.newproject.createDefaultSettings

class PyLanguageEnvironmentCatalogProvider : LanguageEnvironmentCatalogProvider<PyLanguageEnvironment> {

  override suspend fun default(): Result<PyLanguageEnvironment, String> {
    return findPath(INTERPRETER_PROPERTY, "Python interpreter").flatMap { sdkPath ->
      createDefaultSettings(sdkPath)
    }
  }

  override suspend fun collectEnvironmentsForCourse(
    course: Course,
    context: UserDataHolder?
  ): Result<LanguageEnvironmentCatalog<PyLanguageEnvironment>, String> {
    val (pyEnvironments, recommendedEnvironment) = collectPyEnvironments(course, context ?: UserDataHolderBase())

    if (recommendedEnvironment == null || pyEnvironments.isEmpty()) {
      return Err("Can't find python interpreter")
    }

    return Ok(LanguageEnvironmentCatalog(recommendedEnvironment, pyEnvironments))
  }

  override val uiKind: EnvironmentUiKind
    get() = EnvironmentUiKind.ComboBox


  companion object {
    private const val INTERPRETER_PROPERTY = "project.python.interpreter"
    const val ALL_VERSIONS = "All versions"
  }
}
