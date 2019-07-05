package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.plugins.gradle.settings.GradleSystemRunningSettings
import org.jetbrains.plugins.gradle.settings.GradleSystemRunningSettings.PreferredTestRunner.GRADLE_TEST_RUNNER
import org.jetbrains.plugins.gradle.settings.GradleSystemRunningSettings.PreferredTestRunner.PLATFORM_TEST_RUNNER

inline fun <T> withGradleTestRunner(project: Project, task: Task, action: () -> T): T? {
  // Android Studio produces own run configurations
  // that cannot be run correctly if `preferredTestRunner` is `GRADLE_TEST_RUNNER` (AS 3.4/2018.3)
  // See https://youtrack.jetbrains.com/issue/EDU-2250
  val testRunner = if (EduUtils.isAndroidStudio()) PLATFORM_TEST_RUNNER else GRADLE_TEST_RUNNER
  val runningSettings = GradleSystemRunningSettings.getInstance()
  val oldTestRunner = runningSettings.preferredTestRunner
  runningSettings.preferredTestRunner = testRunner
  return try {
    action()
  } finally {
    runningSettings.preferredTestRunner = oldTestRunner
  }
}
