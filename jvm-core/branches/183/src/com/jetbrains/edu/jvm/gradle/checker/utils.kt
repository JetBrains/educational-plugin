package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.plugins.gradle.settings.GradleSystemRunningSettings

inline fun <T> withGradleTestRunner(project: Project, task: Task, action: () -> T): T? {
  val runningSettings = GradleSystemRunningSettings.getInstance()
  val oldTestRunner = runningSettings.preferredTestRunner
  runningSettings.preferredTestRunner = GradleSystemRunningSettings.PreferredTestRunner.GRADLE_TEST_RUNNER
  return try {
    action()
  } finally {
    runningSettings.preferredTestRunner = oldTestRunner
  }
}
