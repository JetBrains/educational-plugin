package com.jetbrains.edu.go.checker

import com.goide.GoConstants.SDK_TYPE_ID
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.go.messages.EduGoBundle
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class GoEnvironmentChecker: EnvironmentChecker() {
  override fun checkEnvironment(project: Project, task: Task): String? {
    val taskDir = task.getDir(project.courseDir) ?: return null
    val module = ModuleUtil.findModuleForFile(taskDir, project) ?: return null
    if (GoSdkService.getInstance(project).getSdk(module) == GoSdk.NULL) {
      return EduGoBundle.message("error.no.sdk", SDK_TYPE_ID)
    }
    return null
  }
}