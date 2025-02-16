package com.jetbrains.edu.cognifiredecompose.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifiredecompose.writers.FunctionWriter

class FunctionGeneratorAction(private val element: PsiElement) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: error("Project was not found")
    handleNewFunction(project)
  }

  private fun handleNewFunction(
    project: Project
  ) {
      runWriteAction {
        FunctionWriter.addFunction(project, element, element.language)
      }
    }

}