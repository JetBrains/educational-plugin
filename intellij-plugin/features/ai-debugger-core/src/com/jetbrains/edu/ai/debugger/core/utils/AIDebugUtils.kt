package com.jetbrains.edu.ai.debugger.core.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.CheckUtils.createTests
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object AIDebugUtils {

  fun CheckResult.failedTestName() = executedTestsInfo.firstFailed()?.name ?: error("No failed test is found")

  fun Project.language() = course?.languageById ?: error("Language is not found")

  fun <T> runWithTests(project: Project, task: Task, execution: () -> T, executionStopped: () -> Unit = {}): T? {
    createTests(task.getInvisibleTestFiles(), project)
    return runCatching {
      execution()
    }.onFailure {
      LOG.error("Failed to start execution")
      executionStopped()
    }.getOrNull()
  }

  fun Task.getInvisibleTestFiles() = taskFiles.values.filter {
    EduUtilsKt.isTestsFile(this, it.name) && !it.isVisible
  }

  private val LOG: Logger = Logger.getInstance(AIDebugUtils::class.java)
}
