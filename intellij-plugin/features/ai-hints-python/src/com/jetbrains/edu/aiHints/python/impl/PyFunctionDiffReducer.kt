package com.jetbrains.edu.aiHints.python.impl

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.aiHints.core.api.FunctionDiffReducer
import com.jetbrains.python.psi.PyFunction

object PyFunctionDiffReducer : FunctionDiffReducer {
  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement): PsiElement {
    val project = modifiedFunction.project
    return if (function != null) {
      reduceDifferenceWithCodeHint(function, modifiedFunction, project)
    }
    else {
      reduceCodeHint(modifiedFunction, project)
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun reduceCodeHint(modifiedFunction: PsiElement, project: Project): PsiElement = modifiedFunction

  private fun reduceDifferenceWithCodeHint(
    current: PsiElement,
    codeHint: PsiElement,
    project: Project
  ): PsiElement = runWriteCommandAction(project) {
    val codeHintBodySize = codeHint.text.lines().size
    if (codeHintBodySize <= 3) return@runWriteCommandAction codeHint

    when {
      current is PyFunction && codeHint is PyFunction -> {
        if (current.parameterList.text != codeHint.parameterList.text) {
          current.parameterList.replace(codeHint.parameterList)
          return@runWriteCommandAction current // Don't make more than one modification in one step
        }

        val currentStatements = current.statementList.statements
        val codeHintStatements = codeHint.statementList.statements
        // For each existing statement, replace with the corresponding from the CodeHint if they differ
        currentStatements.zip(codeHintStatements).forEach { (currentStatement, codeHintStatement) ->
          if (currentStatement.text != codeHintStatement.text) {
            currentStatement.replace(codeHintStatement)
            return@runWriteCommandAction current // Don't make more than one modification in one step
          }
        }

        // No existing statements has been replaced, lets add new statements
        for (i in currentStatements.size until codeHintStatements.size) {
          current.statementList.add(codeHintStatements[i])
          break // Don't make more than one modification in one step
        }
      }
    }

    return@runWriteCommandAction current
  }

  private fun <R> runWriteCommandAction(project: Project, action: () -> R): R {
    var result: R? = null
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      result = action()
    })
    @Suppress("UNCHECKED_CAST")
    return result as R
  }
}