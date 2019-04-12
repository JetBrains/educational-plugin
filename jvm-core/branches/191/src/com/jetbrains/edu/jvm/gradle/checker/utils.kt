package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.settings.TestRunner

inline fun <T> withGradleTestRunner(project: Project, task: Task, action: () -> T): T? {
  val taskDir = task.getTaskDir(project) ?: return null
  val module = ModuleUtil.findModuleForFile(taskDir, project) ?: return null
  val path = ExternalSystemApiUtil.getExternalRootProjectPath(module) ?: return null
  val settings = GradleSettings.getInstance(project).getLinkedProjectSettings(path) ?: return null

  val oldValue = settings.testRunner
  settings.testRunner = TestRunner.GRADLE

  return try {
    action()
  } finally {
    settings.testRunner = oldValue
  }
}
