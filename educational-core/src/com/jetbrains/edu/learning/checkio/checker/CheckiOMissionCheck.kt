package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResult.Companion.failedToCheck
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.notifications.errors.handlers.CheckiOErrorHandler
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.io.IOException
import java.util.concurrent.Callable
import javax.swing.JComponent

abstract class CheckiOMissionCheck(val project: Project,
                                   val task: Task,
                                   private val oAuthConnector: CheckiOOAuthConnector,
                                   private val interpreterName: String,
                                   private val testFormTargetUrl: String
) : Callable<CheckResult> {

  abstract fun getPanel(): JComponent

  @Throws(InterruptedException::class, NetworkException::class)
  protected abstract fun doCheck(): CheckResult

  override fun call(): CheckResult {
    return try {
      doCheck()
    }
    catch (e: InterruptedException) {
      CheckResult(CheckStatus.Unchecked, "Checking was cancelled")
    }
    catch (e: Exception) {
      CheckiOErrorHandler("Failed to check the task", oAuthConnector).handle(e)
      failedToCheck
    }
  }

  protected fun getResources() = mapOf(
    "testFormTargetUrl" to testFormTargetUrl,
    "accessToken" to oAuthConnector.accessToken,
    "taskId" to task.id.toString(),
    "interpreterName" to interpreterName,
    "code" to getCodeFromTask()
  )

  private fun getCodeFromTask(): String {
    val taskFile = (task as CheckiOMission).taskFile
    val missionDir = task.getDir(project)
                     ?: throw IOException("Directory is not found for mission: ${task.id}, ${task.name}")
    val virtualFile = EduUtils.findTaskFileInDir(taskFile, missionDir)
                      ?: throw IOException("Virtual file is not found for mission: ${task.id}, ${task.name}")

    val document = ApplicationManager.getApplication().runReadAction(
      Computable {
        FileDocumentManager.getInstance().getDocument(virtualFile)
      }
    ) ?: throw IOException("Document isn't provided for VirtualFile: ${virtualFile.name}")

    return document.text
  }

  companion object {
    protected const val CHECKIO_TEST_FORM_TEMPLATE = "checkioTestForm.html"
  }
}
