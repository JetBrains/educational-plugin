package com.jetbrains.edu.aiHints.python.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.api.FunctionDiffReducer
import com.jetbrains.python.psi.*

object PyFunctionDiffReducer : FunctionDiffReducer {
  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement): PsiElement {
    return if (function != null) {
      val pyFunction = function.asSafely<PyFunction>() ?: return modifiedFunction // todo: revise this
      val pyCodeHintFunction = modifiedFunction.asSafely<PyFunction>() ?: return modifiedFunction
      reduceDifferenceWithCodeHint(pyFunction, pyCodeHintFunction)
    }
    else {
      modifiedFunction // todo: reduce the newly added function
    }
  }

  private fun reduceDifferenceWithCodeHint(
    current: PyFunction,
    codeHint: PyFunction
  ): PsiElement = runReadAction {
    // Return when either parameter list or return type have been replaced
    if (current.parameterList.replaceIfNeeded(codeHint.parameterList) || current.annotation.replaceIfNeeded(codeHint.annotation)) {
      return@runReadAction current
    }

    // For each existing statement, replace with the corresponding from the CodeHint if they differ
    val currentStatements = current.statementList.statements
    val codeHintStatements = codeHint.statementList.statements
    for ((currentStatement, codeHintStatement) in currentStatements.zip(codeHintStatements)) {
      if (currentStatement.compareNormalized(codeHintStatement)) continue
      when (codeHintStatement) {
        is PyWhileStatement, is PyForStatement, is PyIfStatement -> currentStatement.replaceIfNeeded(codeHintStatement)
        is PyAssignmentStatement, is PyReturnStatement -> currentStatement.replaceWithWriteCommandAction(codeHintStatement)
        else -> currentStatement.replaceWithWriteCommandAction(codeHintStatement)
      }
      return@runReadAction current // Don't make more than one modification in one step
    }

    // As no existing statements have been replaced, let's add the next statement if the Code Hint has one
    if (currentStatements.size < codeHintStatements.size) {
      current.statementList.addReduced(codeHintStatements[currentStatements.size])
    }

    return@runReadAction current
  }

  private fun PsiElement.replaceWithWriteCommandAction(element: PsiElement): PsiElement = runWriteCommandAction(project) {
    replace(element)
  }

  private fun <R> runWriteCommandAction(project: Project, action: () -> R): R {
    var result: R? = null
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      result = action()
    })
    @Suppress("UNCHECKED_CAST")
    return result as R
  }

  private fun <T : PyStatement> PyStatement.replaceIfNeeded(codeHintStatement: T): Boolean {
    // For example, if the current statement is not `for`, replace with `for` part and `pass` statement as a body
    if (this !is PyWhileStatement && this !is PyForStatement && this !is PyIfStatement) {
      val codeHintMainPart = codeHintStatement.mainPart
      runWriteCommandAction(project) {
        codeHintMainPart.statementList.deleteChildRange(codeHintMainPart.statementList.firstChild, codeHintMainPart.statementList.lastChild)
        codeHintMainPart.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
        replace(codeHintMainPart)
      }
      return true
    }
    // The current statement is one of the `PyWhileStatement`, `PyForStatement` or `PyIfStatement`
    if (replaceIfNeeded(mainPart, codeHintStatement.mainPart)) {
      return true
    }
    return false
  }

  private val PyStatement.mainPart: PyStatementPart
    get() = when (this) {
      is PyIfStatement -> ifPart
      is PyForStatement -> forPart
      is PyWhileStatement -> whilePart
      else -> error("Unexpected statement type: ${this::class.java}")
    }

  private fun PyStatementList.addReduced(statement: PyStatement) {
    when (statement) {
      is PyWhileStatement -> {
        val codeHintWhileStatement = statement.whilePart
        runWriteCommandAction(project) {
          codeHintWhileStatement.statementList.deleteChildRange(
            codeHintWhileStatement.statementList.firstChild,
            codeHintWhileStatement.statementList.lastChild
          )
          codeHintWhileStatement.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
          statement.whilePart.statementList.children.forEach { it.delete() }
          statement.whilePart.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
          add(codeHintWhileStatement)
        }
      }

      is PyForStatement, is PyIfStatement -> {
        val codeHintForStatement = statement.mainPart
        runWriteCommandAction(project) {
          codeHintForStatement.statementList.deleteChildRange(
            codeHintForStatement.statementList.firstChild,
            codeHintForStatement.statementList.lastChild
          )
          codeHintForStatement.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
          add(codeHintForStatement)
        }
      }

      else -> runWriteCommandAction(project) {
        add(statement)
      }
    }
  }

  private fun replaceIfNeeded(
    currentPyStatementPart: PyStatementPart,
    codeHintPyStatementPart: PyStatementPart,
  ): Boolean {
    val project = currentPyStatementPart.project
    if (currentPyStatementPart.compareNormalized(codeHintPyStatementPart)) return false

    when (currentPyStatementPart) {
      is PyWhilePart, is PyIfPart -> {
        val currentBinaryExpression = currentPyStatementPart.children.firstOrNull { it is PyBinaryExpression } ?: return false
        val codeHintBinaryExpression = codeHintPyStatementPart.children.firstOrNull { it is PyBinaryExpression } ?: return false
        if (!currentBinaryExpression.compareNormalized(codeHintBinaryExpression)) {
          currentBinaryExpression.replaceWithWriteCommandAction(codeHintBinaryExpression)
          return true
        }
      }

      is PyForPart -> {
        val currentForPart = currentPyStatementPart.copy() as PyForPart
        val codeHintForPart = codeHintPyStatementPart.copy() as PyForPart
        runWriteCommandAction(project) {
          currentForPart.lastChild.delete()
          codeHintForPart.lastChild.delete()
        }
        if (!currentForPart.compareNormalized(codeHintForPart)) {
          runWriteCommandAction(project) {
            currentForPart.replace(codeHintForPart)
            codeHintForPart.add(currentPyStatementPart.statementList)
            currentPyStatementPart.replace(codeHintForPart)
          }
          return true
        }
      }
    }

    val currentStatements = currentPyStatementPart.statementList.statements
    val codeHintStatements = codeHintPyStatementPart.statementList.statements
    currentStatements.zip(codeHintStatements).forEach { (currentStatement, codeHintStatement) ->
      if (!currentStatement.compareNormalized(codeHintStatement)) {
        currentStatement.replaceWithWriteCommandAction(codeHintStatement)
        return true
      }
    }
    if (currentStatements.size < codeHintStatements.size) {
      runWriteCommandAction(project) {
        currentPyStatementPart.statementList.add(codeHintStatements[currentStatements.size])
      }
      return true
    }
    return false
  }

  private fun PyParameterList.replaceIfNeeded(codeHintParameterList: PyParameterList): Boolean {
    if (!compareNormalized(codeHintParameterList)) {
      replaceWithWriteCommandAction(codeHintParameterList)
      return true
    }
    return false
  }

  private fun PyAnnotation?.replaceIfNeeded(codeHintAnnotation: PyAnnotation?): Boolean {
    val currentAnnotation = this ?: return false
    if (codeHintAnnotation == null) {
      runWriteCommandAction(project) {
        currentAnnotation.delete()
      }
      return true
    }
    if (!compareNormalized(codeHintAnnotation)) {
      currentAnnotation.replaceWithWriteCommandAction(codeHintAnnotation)
      return true
    }
    return false
  }

  private fun PsiElement.compareNormalized(psiElement: PsiElement): Boolean {
    val currentTextNormalized = text.replace(" ", "")
    val anotherTextNormalized = psiElement.text.replace(" ", "")
    return currentTextNormalized == anotherTextNormalized
  }
}