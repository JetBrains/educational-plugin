package com.jetbrains.edu.ai.debugger.core.utils

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.CheckUtils.createTests
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.debugger.dto.ProgrammingLanguage
import com.jetbrains.educational.ml.debugger.dto.TaskDescriptionFormat

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

  fun List<TaskFile>.toNameTextMap(project: Project) = runReadAction { associate { it.name to (it.getText(project) ?: "") } }

  fun DescriptionFormat.toTaskDescriptionType() =
    when (this) {
      DescriptionFormat.MD -> TaskDescriptionFormat.MD
      DescriptionFormat.HTML -> TaskDescriptionFormat.HTML
    }

  fun Task.getTaskDescriptionText(project: Project) = runReadAction {
    getDescriptionFile(project)?.readText()
  } ?: error("There are no description for the task")

  fun Project.getLanguage() =
    when (course?.languageId) {
      "kotlin" -> ProgrammingLanguage.KOTLIN
      "JAVA" -> ProgrammingLanguage.JAVA
      else -> error("Language is not supported")
    }

}
