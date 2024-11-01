package com.jetbrains.edu.android.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.android.messages.EduAndroidBundle
import com.jetbrains.edu.jvm.gradle.checker.GradleEnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class AndroidEnvironmentChecker : GradleEnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val result = super.getEnvironmentError(project, task)
    if (result != null) return result

    if (task is EduTask) {
      val hasInstrumentedTests = task.taskFiles.any { (path, _) ->
        path.startsWith("src/androidTest") && !path.endsWith("AndroidEduTestRunner.kt")
      }
      if (hasInstrumentedTests) {
        if (!AndroidDeviceHelper.getInstance(project).hasDevices()) {
          return CheckResult(CheckStatus.Unchecked, EduAndroidBundle.message("error.no.emulator.message"), hyperlinkAction = {
            AndroidDeviceHelper.getInstance(project).createDeviceIfNeeded()
          })
        }
      }
    }
    return null
  }
}
