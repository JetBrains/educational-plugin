package com.jetbrains.edu.scala.sbt.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.scala.messages.EduScalaBundle
import org.jetbrains.sbt.SbtUtil

class ScalaSbtEnvironmentChecker : EnvironmentChecker() {
  override fun checkEnvironment(project: Project, task: Task): CheckResult? {
    if (ProjectRootManager.getInstance(project).projectSdk == null) {
      return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("error.no.sdk"))
    }
    if (!SbtUtil.isSbtProject(project)) {
      return CheckResult(CheckStatus.Unchecked, EduScalaBundle.message("error.no.sbt.project"))
    }
    return null
  }
}
