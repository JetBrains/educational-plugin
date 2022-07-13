package com.jetbrains.edu.scala.sbt.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_SCALA
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.scala.messages.EduScalaBundle
import org.jetbrains.sbt.SbtUtil

class ScalaSbtEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    if (ProjectRootManager.getInstance(project).projectSdk == null) {
      return CheckResult(CheckStatus.Unchecked, EduScalaBundle.message("error.no.sdk", ENVIRONMENT_CONFIGURATION_LINK_SCALA))
    }
    if (!SbtUtil.isSbtProject(project)) {
      return CheckResult(CheckStatus.Unchecked, EduScalaBundle.message("error.no.sbt.project", ENVIRONMENT_CONFIGURATION_LINK_SCALA))
    }
    return null
  }
}
