package com.jetbrains.edu.aiHints.python.impl

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.aiHints.core.api.FunctionDiffReducer
import com.jetbrains.python.psi.*

object PyFunctionDiffReducer : FunctionDiffReducer<PyFunction> {
  override fun reduceDiffFunctions(project: Project, currentFunction: PyFunction?, codeHintFunction: PyFunction): PyFunction {
    return runWriteCommandAction(project) {
      currentFunction?.reduceDifferenceWith(codeHint = codeHintFunction) ?: codeHintFunction // todo: reduce the newly added function
    }
  }

  @RequiresReadLock
  private fun PyFunction.reduceDifferenceWith(codeHint: PyFunction): PyFunction {
    // Check parameters and return type of the functions
    if (unifyParameters(codeHint) || unifyReturnType(codeHint)) {
      return this // Don't make more than one modification in one step
    }

    // Find the first difference in the body statements
    val foundDifference = firstDifferentStatement(codeHint)
    if (foundDifference != null) {
      val (currentStatement, codeHintStatement) = foundDifference
      when (codeHintStatement) {
        is PyWhileStatement, is PyForStatement, is PyIfStatement -> currentStatement.replaceIfNeeded(codeHintStatement)
        is PyAssignmentStatement, is PyReturnStatement -> currentStatement.replace(project, codeHintStatement)
        else -> currentStatement.replace(project, codeHintStatement)
      }
      return this // Don't make more than one modification in one step
    }

    // If no difference was found, let's add the first new statement from the Code Hint
    val nextStatement = findNextFrom(codeHint)
    if (nextStatement != null) {
      addNewStatement(nextStatement)
    }
    return this
  }

  /**
   * Checks if functions have different lists of parameters and applies the list from the CodeHint to the given function.
   *
   * @return [Boolean] whether the modification was made
   */
  private fun PyFunction.unifyParameters(codeHintFunction: PyFunction): Boolean {
    val codeHintParameterList = codeHintFunction.parameterList
    if (!parameterList.compareNormalized(codeHintParameterList)) {
      runWriteCommandAction(project) {
        parameterList.replace(codeHintParameterList)
      }
      return true
    }
    return false
  }

  /**
   * Checks if functions have return type annotations and applies the annotation from the CodeHint to the given function.
   *
   * @return [Boolean] whether the modification was made
   */
  private fun PyFunction.unifyReturnType(codeHintFunction: PyFunction): Boolean {
    val currentAnnotation = annotation ?: return false
    val codeHintAnnotation = codeHintFunction.annotation
    if (codeHintAnnotation == null) {
      runWriteCommandAction(project) {
        currentAnnotation.delete()
      }
      return true
    }
    if (!currentAnnotation.compareNormalized(codeHintAnnotation)) {
      runWriteCommandAction(project) {
        currentAnnotation.replace(codeHintAnnotation)
      }
      return true
    }
    return false
  }

  private fun <T : PyStatement> PyStatement.replaceIfNeeded(codeHintStatement: T): Boolean {
    // For example, if the current statement is not `for`, replace with `for` part and `pass` statement as a body
    if (this !is PyWhileStatement && this !is PyForStatement && this !is PyIfStatement) {
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

  private fun PyFunction.addNewStatement(statement: PyStatement) {
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
          statementList.add(codeHintWhileStatement)
        }
      }

      is PyForStatement, is PyIfStatement -> {
        val codeHintForStatement = statement.mainPart
        runWriteCommandAction(project) {
          codeHintForStatement.statementList.children.forEach { it.delete() }
          statementList.add(codeHintForStatement)
        }
      }

      else -> runWriteCommandAction(project) {
        statementList.add(statement)
      }
    }
  }

  private fun replaceIfNeeded(
    currentPyStatementPart: PyStatementPart?,
    codeHintPyStatementPart: PyStatementPart?,
  ): Boolean {
    if (currentPyStatementPart == null || codeHintPyStatementPart == null) return false

    val project = currentPyStatementPart.project
    if (currentPyStatementPart.compareNormalized(codeHintPyStatementPart)) return false

    when (currentPyStatementPart) {
      is PyWhilePart, is PyIfPart -> {
        val currentBinaryExpression = currentPyStatementPart.children.firstOrNull { it is PyBinaryExpression } ?: return false
        val codeHintBinaryExpression = codeHintPyStatementPart.children.firstOrNull { it is PyBinaryExpression } ?: return false
        if (!currentBinaryExpression.compareNormalized(codeHintBinaryExpression)) {
          currentBinaryExpression.replace(project, codeHintBinaryExpression)
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

    val firstDifference = currentPyStatementPart.firstDifferentStatement(codeHintPyStatementPart)
    if (firstDifference != null) {
      val (currentStatement, codeHintStatement) = firstDifference
      currentStatement.replace(project, codeHintStatement)
      return true
    }
    val nextStatement = currentPyStatementPart.findNextFrom(codeHintPyStatementPart)
    if (nextStatement != null) {
      runWriteCommandAction(project) {
        currentPyStatementPart.statementList.add(nextStatement)
      }
      return true
    }
    return false
  }
}