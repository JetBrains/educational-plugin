package com.jetbrains.edu.coursecreator.validation

import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.ext.getFormattedTaskText
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionLink
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.util.concurrent.ConcurrentHashMap

class CourseValidationHelper(private val params: ValidationParams) {

  suspend fun validate(project: Project, course: Course): ValidationSuite {
    return withValidationEnabled(project) {
      withValidationTreeBuilder(ValidationResultNode.ROOT_NODE_NAME) {
        validate(project, course)
      }
    }
  }

  private suspend fun ValidationTreeBuilder.validate(project: Project, container: ItemContainer) {
    LOG.info("Validating `${container.name}` course item")
    return validationSuit(container.name) {
      container.items.map { item ->
        when (item) {
          is ItemContainer -> {
            validate(project, item)
          }
          is Task -> {
            validate(project, item)
          }
          else -> error("Unreachable")
        }
      }
    }
  }

  private suspend fun ValidationTreeBuilder.validate(project: Project, task: Task) {
    LOG.info("Validating `${task.presentableName}` task")
    validationSuit(task.presentableName) {
      withContext(Dispatchers.EDT) {
        task.prepareForChecking(project)
      }

      try {
        if (params.validateTests) {
          validateTaskTests(project, task)
        }
        if (params.validateLinks) {
          validateTaskDescriptionLinks(project, task)
        }
      }
      finally {
        withContext(Dispatchers.EDT) {
          closeOpenFiles(project)
        }
      }
    }
  }

  private suspend fun ValidationTreeBuilder.validateTaskTests(project: Project, task: Task) {
    LOG.info("Validating `${task.presentableName}` task's tests")
    withContext(Dispatchers.EDT) {
      val dataContext = SimpleDataContext.getProjectContext(project)
      ActionUtil.invokeAction(CheckAction(), dataContext, "", null, null)
    }

    val result = ValidationCheckResultManager.getInstance(project).getResult(task)

    val caseResult = when (result.status) {
      CheckStatus.Solved -> ValidationCaseResult.Success
      CheckStatus.Unchecked -> ValidationCaseResult.Ignored(result.message)
      CheckStatus.Failed -> ValidationCaseResult.Failed(result.message, result.details, result.diff?.let { ValidationDiff(it.expected, it.actual) })
    }

    LOG.info("Validating `${task.presentableName}` task's tests: ${result.status}")
    validationCase(TESTS_NODE, caseResult)
  }

  private suspend fun ValidationTreeBuilder.validateTaskDescriptionLinks(project: Project, task: Task) {
    LOG.info("Validating `${task.presentableName}` task's links")

    val text = readAction { task.getFormattedTaskText(project) } ?: return
    val links = extractLinks(project, task, text)
    // Don't create empty node if no links in the task description
    if (links.isEmpty()) return

    validationSuit(TASK_DESCRIPTION_LINKS_NODE) {
      val results = withContext(Dispatchers.IO) {
        links.map {
          async { it to it.validate(project) }
        }.awaitAll()
      }

      for ((link, error) in results) {
        val taskDescriptionLink = link.link

        val result = if (error == null) {
          ValidationCaseResult.Success
        }
        else {
          ValidationCaseResult.Failed("Failed to resolve `$taskDescriptionLink`", error)
        }

        LOG.info("Validation `$taskDescriptionLink`: ${if (error == null) "Success" else "Failed"}")
        validationCase(taskDescriptionLink, result)
      }
    }
  }

  private fun extractLinks(project: Project, task: Task, text: String): List<TaskDescriptionLink<*, *>> {
    val document = Jsoup.parse(text)
    val elements = document.select("a[href], img")

    return elements.flatMap {
      when (it.tagName()) {
        "a" ->  {
          val linkText = it.attr("href")
          listOfNotNull(TaskDescriptionLink.fromUrl(linkText))
        }
        "img" -> ImgLink.collectImageLinks(project, task, it)
        else -> emptyList()
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

  companion object {
    private const val TESTS_NODE = "Tests"
    private const val TASK_DESCRIPTION_LINKS_NODE = "Task description links"

    private val LOG = logger<CourseValidationHelper>()
  }
}

@Service(Service.Level.PROJECT)
private class ValidationCheckResultManager {

  private val results: MutableMap<Task, CheckResult> = ConcurrentHashMap()

  @Volatile
  var validationEnabled: Boolean = false

  fun putCheckResult(task: Task, result: CheckResult) {
    require(task !in results) {
      "Task `${task.pathInCourse}` is already checked"
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
