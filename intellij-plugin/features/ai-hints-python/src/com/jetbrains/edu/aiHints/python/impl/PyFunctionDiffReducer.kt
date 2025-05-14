package com.jetbrains.edu.aiHints.python.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.aiHints.core.api.FunctionDiffReducer
import com.jetbrains.python.psi.*

private const val SMALL_FUNCTION_SIZE: Int = 3

object PyFunctionDiffReducer : FunctionDiffReducer {
  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement): PsiElement {
    val project = runReadAction { modifiedFunction.project }
    val codeHint = modifiedFunction.asSafely<PyFunction>() ?: return modifiedFunction
    if (function != null) {
      val currentFunction = function.asSafely<PyFunction>() ?: return modifiedFunction
      return runWriteCommandAction(project) {
        currentFunction.reduceDifferenceWith(project, codeHint)
      }
    }
    return reduce(project, codeHint)
  }

  private fun reduce(project: Project, codeHint: PyFunction): PyFunction {
    val functionSize = runReadAction { codeHint.text.lines().size }
    if (functionSize <= SMALL_FUNCTION_SIZE) return codeHint
    runWriteCommandAction(project) {
      codeHint.statementList.children.forEach { it.delete() }
    }
    return codeHint
  }

  @RequiresReadLock
  private fun PyFunction.reduceDifferenceWith(project: Project, codeHint: PyFunction): PyFunction {
    if (codeHint.text.lines().size <= SMALL_FUNCTION_SIZE) return codeHint

    // Check parameters and return type of the functions
    if (parameterList.deleteOrSwapWith(project, codeHint.parameterList) || annotation.deleteOrSwapWith(project, codeHint.annotation)) {
      return this // Don't make more than one modification in one step
    }

    unifyStatementLists(this, codeHint)
    return this
  }

  private fun unifyStatementLists(
    currentPyStatementList: PyStatementListContainer,
    codeHintPyStatementList: PyStatementListContainer,
  ): Boolean {
    // Find the first difference in the list statements
    val firstDifference = currentPyStatementList.firstDifferentStatement(codeHintPyStatementList)
    if (firstDifference != null) {
      val (currentStatement, codeHintStatement) = firstDifference
      currentStatement.unifyStatementWith(codeHintStatement)
      return true
    }
    // If no difference was found, let's add the first new statement from the Code Hint list statements
    val nextStatement = currentPyStatementList.findNextFrom(codeHintPyStatementList)
    if (nextStatement != null) {
      currentPyStatementList.addNewStatement(nextStatement)
      return true
    }
    return false
  }

  private fun PyStatement.unifyStatementWith(another: PyStatement) {
    val (currentStatement, codeHintStatement) = this to another
    when (codeHintStatement) {
      is PyWhileStatement, is PyForStatement, is PyIfStatement -> currentStatement.replaceIfNeeded(codeHintStatement)
      is PyAssignmentStatement, is PyReturnStatement -> currentStatement.replace(project, codeHintStatement)
      else -> currentStatement.replace(project, codeHintStatement)
    }
  }

  private fun <T : PyStatement> PyStatement.replaceIfNeeded(codeHintStatement: T): Boolean {
    // For example, if the current statement is not `for`, replace with `for` part and `pass` statement as a body
    if (this::class != codeHintStatement::class) {
      val codeHintMainPart = codeHintStatement.mainPart
      runWriteCommandAction(project) {
        codeHintMainPart.statementList.children.forEach { it.delete() }
        replace(codeHintMainPart)
      }
      return true
    }
    // The current statement is one of the `PyWhileStatement`, `PyForStatement` or `PyIfStatement`
    if (replaceIfNeeded(mainPart, codeHintStatement.mainPart)) {
      return true
    }

    if (this is PyIfStatement && codeHintStatement is PyIfStatement && modifyElifParts(codeHintStatement)) {
      return true
    }

    if (replaceIfNeeded(elsePart, codeHintStatement.elsePart)) {
      return true
    }
    if (elsePart == null) {
      val codeHintElsePart = codeHintStatement.elsePart ?: return false
      // Add else part with `pass` statement as a body if the Code Hint has one
      reduceAndAdd(codeHintElsePart)
      return true
    }
    return false
  }

  private fun PyIfStatement.modifyElifParts(codeHintStatement: PyIfStatement): Boolean {
    val currentElifParts = elifParts
    val codeHintElifParts = codeHintStatement.elifParts

    if (currentElifParts.isNotEmpty() && codeHintElifParts.isNotEmpty()) {
      for ((currentElifPart, codeHintElifPart) in currentElifParts.zip(codeHintElifParts)) {
        if (currentElifPart.compareNormalized(codeHintElifPart)) continue
        if (replaceIfNeeded(currentElifPart, codeHintElifPart)) {
          return true
        }
      }
      // Insert next `elif` if possible
      if (currentElifParts.size < codeHintElifParts.size) {
        val reducedElifPart = codeHintElifParts[currentElifParts.size].copy() as PyIfPartElif
        reduceAndAdd(reducedElifPart, elsePart)
        return true
      }
    }
    else if (currentElifParts.isEmpty() && codeHintElifParts.isNotEmpty()) {
      // Add the first elif part (reduced) from CodeHint
      val reducedElifPart = codeHintElifParts[0].copy() as PyIfPartElif
      reduceAndAdd(reducedElifPart, elsePart)
      return true
    }
    else if (currentElifParts.isNotEmpty()) { // CodeHint's elif parts are empty
      // Removing all `elif` parts
      runWriteCommandAction(project) {
        deleteChildRange(currentElifParts.first(), currentElifParts.last())
      }
      return true
    } // else: both are empty, do nothing

    return false
  }

  private fun PyStatementListContainer.addNewStatement(statement: PyStatement) = when (statement) {
    is PyWhileStatement, is PyForStatement, is PyIfStatement -> {
      val codeHintMainPart = statement.mainPart
      runWriteCommandAction(project) {
        codeHintMainPart.statementList.children.forEach { it.delete() }
        statementList.add(codeHintMainPart)
      }
    }

    else -> runWriteCommandAction(project) {
      statementList.add(statement)
    }
  }

  private fun replaceIfNeeded(
    currentPyStatementPart: PyStatementPart?,
    codeHintPyStatementPart: PyStatementPart?,
  ): Boolean {
    if (currentPyStatementPart == null || codeHintPyStatementPart == null) return false

    val project = currentPyStatementPart.project
    if (currentPyStatementPart.compareNormalized(codeHintPyStatementPart)) return false

    return when {
      currentPyStatementPart is PyConditionalStatementPart && codeHintPyStatementPart is PyConditionalStatementPart -> {
        currentPyStatementPart.condition.deleteOrSwapWith(project, codeHintPyStatementPart.condition)
        || unifyStatementLists(currentPyStatementPart, codeHintPyStatementPart)
      }

      currentPyStatementPart is PyElsePart && codeHintPyStatementPart is PyElsePart -> {
        unifyStatementLists(currentPyStatementPart, codeHintPyStatementPart)
      }

      currentPyStatementPart is PyForPart && codeHintPyStatementPart is PyForPart -> {
        currentPyStatementPart.target.deleteOrSwapWith(project, codeHintPyStatementPart.target)
        || currentPyStatementPart.source.deleteOrSwapWith(project, codeHintPyStatementPart.source)
        || unifyStatementLists(currentPyStatementPart, codeHintPyStatementPart)
      }

      else -> false
    }
  }
}