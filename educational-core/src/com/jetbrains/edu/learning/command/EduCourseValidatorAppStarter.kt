package com.jetbrains.edu.learning.command

import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class EduCourseValidatorAppStarter : EduCourseProjectAppStarterBase() {
  override val commandName: String
    get() = "validateCourse"

  override val courseMode: CourseMode
    get() = CourseMode.EDUCATOR

  override suspend fun performProjectAction(project: Project, course: Course): CommandResult {
    val results = mutableMapOf<Task, CheckResult>()
    withValidationEnabled(project) {
      course.check(project, results)
    }

    val hasFailedTasks = results.values.any { !it.isSolved }

    return if (hasFailedTasks) CommandResult.Error("Some tasks haven't finished successfully") else CommandResult.Ok
  }

  private suspend fun ItemContainer.check(project: Project, results: MutableMap<Task, CheckResult>) {
    testSuit(name) {
      for (item in items) {
        when (item) {
          is ItemContainer -> {
            item.check(project, results)
          }

          is Task -> {
            results[item] = item.check(project)
          }
        }
      }
    }
  }

  private suspend fun <T> testSuit(name: String, action: suspend () -> T): T {
    println(ServiceMessageBuilder.testSuiteStarted(name))
    return try {
      action()
    }
    finally {
      println(ServiceMessageBuilder.testSuiteFinished(name))
    }
  }

  private suspend fun Task.check(project: Project): CheckResult {
    println(ServiceMessageBuilder.testStarted(presentableName))
    withContext(Dispatchers.EDT) {
      prepareForChecking(project)
      val dataContext = SimpleDataContext.getProjectContext(project)
      ActionUtil.invokeAction(CheckAction(), dataContext, "", null, null)
    }

    val result = ValidationCheckResultManager.getInstance(project).getResult(this)

    withContext(Dispatchers.EDT) {
      closeOpenFiles(project)
    }

    val testMessage = when (result.status) {
      CheckStatus.Unchecked -> {
        ServiceMessageBuilder.testIgnored(presentableName)
          .addAttribute("message", result.message)
      }
      CheckStatus.Solved -> null
      CheckStatus.Failed -> {
        ServiceMessageBuilder.testFailed(presentableName)
          .addAttribute("message", result.message)
          .addAttribute("details", result.details.orEmpty())
      }
    }
    if (testMessage != null) {
      println(testMessage)
    }
    println(ServiceMessageBuilder.testFinished(presentableName))
    return result
  }

  @RequiresEdt
  private fun Task.prepareForChecking(project: Project) {
    // TODO: properly open tasks inside framework lessons
    val taskFile = taskFiles.values.first()
    val virtualFile = taskFile.getVirtualFile(project)
                      ?: error("Can't find virtual file for `${taskFile.name}` task file in `$name task`")

    FileEditorManager.getInstance(project).openFile(virtualFile, true)
  }

  @RequiresEdt
  private fun closeOpenFiles(project: Project) {
    val fileEditorManager = FileEditorManager.getInstance(project)
    for (openFile in fileEditorManager.openFiles) {
      fileEditorManager.closeFile(openFile)
    }
  }
}

@Service(Service.Level.PROJECT)
private class ValidationCheckResultManager {

  private val results: MutableMap<Task, CheckResult> = ConcurrentHashMap()

  @Volatile
  var validationEnabled: Boolean = false

  fun putCheckResult(task: Task, result: CheckResult) {
    require(task !in results)

    results[task] = result
  }

  suspend fun getResult(task: Task): CheckResult {
    while (task !in results) {
      delay(1000)
    }
    return results.getValue(task)
  }

  companion object {
    fun getInstance(project: Project): ValidationCheckResultManager = project.service()
  }
}

private suspend fun <T> withValidationEnabled(project: Project, action: suspend () -> T): T {
  val validationManager = ValidationCheckResultManager.getInstance(project)
  validationManager.validationEnabled = true
  return try {
    action()
  }
  finally {
    validationManager.validationEnabled = false
  }
}

class ValidationCheckListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val validationManager = ValidationCheckResultManager.getInstance(project)
    if (validationManager.validationEnabled) {
      validationManager.putCheckResult(task, result)
    }
  }
}
