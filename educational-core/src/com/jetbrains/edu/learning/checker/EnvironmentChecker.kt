package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle

open class EnvironmentChecker {
  protected open fun checkEnvironment(project: Project, task: Task): String? = null

  /**
   * @return null means that there are no problems with environment
   */
  fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val possibleError = checkEnvironment(project, task) ?: return null
    return CheckResult(CheckStatus.Unchecked, "$possibleError\n${EduCoreBundle.message("help.use.guide", EduNames.NO_TESTS_URL)}")
  }
}