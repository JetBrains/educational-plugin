package com.jetbrains.edu.cognifiredecompose.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifiredecompose.messages.EduCognifireDecomposeBundle
import com.jetbrains.edu.cognifiredecompose.writers.FunctionWriter
import com.jetbrains.edu.learning.actions.EduActionUtils

class FunctionGeneratorAction(private val element: PsiElement) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: error("Project was not found")
    val document = getDocument() ?: return

    document.setReadOnly(true)
    executeAction(project)
  }

  private fun getDocument() = runReadAction { FileDocumentManager.getInstance().getDocument(element.containingFile.virtualFile) }

  private fun executeAction(project: Project) = runBackgroundableTask(
    EduCognifireDecomposeBundle.message("action.progress.bar.message"),
    project
  ) { indicator ->
    runLocked() {
      runWithProgressBar(indicator) {
        handleNewFunction(project)
      }
    }
  }


  private fun runLocked(execution: () -> Unit) {
    try {
      execution()
    } catch (_: Throwable) {
      error("action.not.run.due.to.unknown.exception")
    }

      finally {
        getDocument()?.setReadOnly(false)
      }
  }

  private fun runWithProgressBar(indicator: ProgressIndicator, execution: () -> Unit) {
    ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }
    execution()
  }

  private fun handleNewFunction(
    project: Project
  ) {
      invokeLater {
        FunctionWriter.addFunction(project, element, element.language)
      }
    }

}