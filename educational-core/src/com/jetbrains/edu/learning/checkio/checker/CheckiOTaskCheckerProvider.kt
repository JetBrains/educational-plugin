package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

open class CheckiOTaskCheckerProvider(
  private val apiConnector: CheckiOApiConnector,
  private val interpreterName: String,
  private val testFormTargetUrl: String
): TaskCheckerProvider {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return CheckiOTaskChecker(
      task,
      project,
      apiConnector.oAuthConnector,
      interpreterName,
      testFormTargetUrl
    )
  }
}