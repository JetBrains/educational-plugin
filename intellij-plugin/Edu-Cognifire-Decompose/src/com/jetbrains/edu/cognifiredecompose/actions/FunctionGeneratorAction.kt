package com.jetbrains.edu.cognifiredecompose.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifiredecompose.messages.EduCognifireDecomposeBundle
import com.jetbrains.edu.cognifiredecompose.writers.FunctionWriter

class FunctionGeneratorAction(private val element: PsiElement) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: error("Project was not found")
    executeAction(project)
  }

  private fun executeAction(project: Project) = runBackgroundableTask(
    EduCognifireDecomposeBundle.message("action.progress.bar.message"),
    project
  ) { _ -> handleNewFunction(project) }

  private fun handleNewFunction(
    project: Project
  ) {
      invokeLater {
        FunctionWriter.addFunction(project, element, element.language)
      }
    }

}