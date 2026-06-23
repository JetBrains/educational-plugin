package com.jetbrains.edu.learning.newproject.environment

import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course


/**
 * Contains logic to detect all available environments suitable for opening a new course project.
 */
interface LanguageEnvironmentCatalogProvider<out E: LanguageEnvironment> {

  /**
   * The kind of UI to select the environment, if the environment list is obtained with the [collectEnvironmentsForCourse] method.
   */
  val uiKind: EnvironmentUiKind

  /**
   * The default environment is used in a controlled environment where we want to use a predefined language environment without any UI
   */
  suspend fun default(): Result<E, String>

  /**
   * Collects all the available environments suitable for the specified course.
   *
   * @return a [Result] containing the collected environments in the form of a [LanguageEnvironmentCatalog] object if successful,
   *         or an error message as a [String] if the operation fails
   */
  suspend fun collectEnvironmentsForCourse(course: Course, context: UserDataHolder?): Result<LanguageEnvironmentCatalog<E>, String>
}