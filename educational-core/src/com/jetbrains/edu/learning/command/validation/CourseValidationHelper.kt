package com.jetbrains.edu.learning.command.validation

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
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.ext.getPathInCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class CourseValidationHelper(private val serviceMessageConsumer: ServiceMessageConsumer) {

  suspend fun validate(project: Project, course: Course): Boolean {
    return withValidationEnabled(project) {
      withTestSuiteBuilder(serviceMessageConsumer) {
        check(project, course)
      }
    }
  }

  private suspend fun TestSuiteBuilder.check(project: Project, itemContainer: ItemContainer) {
    testSuite(itemContainer.name) {
      for (item in itemContainer.items) {
        when (item) {
          is ItemContainer -> {
            check(project, item)
          }
          is Task -> {
            check(project, item)
          }
          else -> error("Unreachable")
        }
      }
    }
  }

  private suspend fun TestSuiteBuilder.check(project: Project, task: Task) {
    testCase(task.presentableName) {
      withContext(Dispatchers.EDT) {
        task.prepareForChecking(project)
        val dataContext = SimpleDataContext.getProjectContext(project)
        ActionUtil.invokeAction(CheckAction(), dataContext, "", null, null)
      }

      val result = ValidationCheckResultManager.getInstance(project).getResult(task)

      withContext(Dispatchers.EDT) {
        closeOpenFiles(project)
      }

      when (result.status) {
        CheckStatus.Unchecked -> testIgnored(result.message)
        CheckStatus.Failed -> testFailed(result.message, result.details.orEmpty())
        CheckStatus.Solved -> Unit
      }
    }
  }

  @RequiresEdt
  private fun Task.prepareForChecking(project: Project) {
    // TODO: properly open tasks inside framework lessons
    val taskFile = taskFiles.values.first()
    val virtualFile = taskFile.getVirtualFile(project)
                      ?: error("Can't find virtual file for `${taskFile.name}` task file in `$name` task")

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
    require(task !in results) {
      "Task `${task.getPathInCourse()}` is already checked"
    }

    results[task] = result
  }

  suspend fun getResult(task: Task): CheckResult {
    require(validationEnabled) {
      "`ValidationCheckResultManager.validationEnabled` should be true. Otherwise, check results are not collected"
    }

    while (task !in results) {
      delay(100)
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
