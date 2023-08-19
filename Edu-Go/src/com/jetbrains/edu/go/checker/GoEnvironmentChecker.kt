package com.jetbrains.edu.go.checker

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.go.messages.EduGoBundle
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_GO
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class GoEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val taskDir = task.getDir(project.courseDir) ?: return null
    val module = ModuleUtil.findModuleForFile(taskDir, project) ?: return null
    if (GoSdkService.getInstance(project).getSdk(module) == GoSdk.NULL) {
      return CheckResult(CheckStatus.Unchecked, EduGoBundle.message("error.no.sdk", ENVIRONMENT_CONFIGURATION_LINK_GO))
    }
    return null
  }
}