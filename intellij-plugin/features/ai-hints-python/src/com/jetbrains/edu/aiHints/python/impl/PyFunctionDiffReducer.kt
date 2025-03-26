package com.jetbrains.edu.aiHints.python.impl

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.api.FunctionDiffReducer
import com.jetbrains.python.psi.*

object PyFunctionDiffReducer : FunctionDiffReducer {
  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement): PsiElement {
    val project = modifiedFunction.project
    return if (function != null) {
      val pyFunction = function.asSafely<PyFunction>() ?: return modifiedFunction // todo: revise this
      val pyCodeHintFunction = modifiedFunction.asSafely<PyFunction>() ?: return modifiedFunction
      reduceDifferenceWithCodeHint(project, pyFunction, pyCodeHintFunction)
    }
    else {
      modifiedFunction // todo: reduce the newly added function
    }
  }

  private fun reduceDifferenceWithCodeHint(
    project: Project,
    current: PyFunction,
    codeHint: PyFunction
  ): PsiElement = runWriteCommandAction(project) {
    // Return when either parameter list or return type have been replaced
    if (current.parameterList.replaceIfNeeded(codeHint.parameterList) || current.annotation.replaceIfNeeded(codeHint.annotation)) {
      return@runWriteCommandAction current
    }

    // For each existing statement, replace with the corresponding from the CodeHint if they differ
    val currentStatements = current.statementList.statements
    val codeHintStatements = codeHint.statementList.statements
    for ((currentStatement, codeHintStatement) in currentStatements.zip(codeHintStatements)) {
      if (currentStatement.text == codeHintStatement.text) continue
      when (codeHintStatement) {
        is PyWhileStatement -> currentStatement.replaceIfNeeded(codeHintStatement)
        is PyForStatement -> currentStatement.replaceIfNeeded(codeHintStatement)
        is PyIfStatement -> currentStatement.replaceIfNeeded(codeHintStatement)
        is PyAssignmentStatement, is PyReturnStatement -> currentStatement.replace(codeHintStatement)
        else -> currentStatement.replace(codeHintStatement)
      }
      return@runWriteCommandAction current // Don't make more than one modification in one step
    }

    // As no existing statements has been replaced, lets add the next statement if the Code Hint has one
    if (currentStatements.size < codeHintStatements.size) {
      current.statementList.addReduced(codeHintStatements[currentStatements.size])
    }

    return@runWriteCommandAction current
  }

  private fun PyStatementList.addReduced(statement: PyStatement) {
    when (statement) {
      is PyWhileStatement -> {
        val codeHintWhileStatement = statement.whilePart
        codeHintWhileStatement.statementList.deleteChildRange(
          codeHintWhileStatement.statementList.firstChild,
          codeHintWhileStatement.statementList.lastChild
        )
        codeHintWhileStatement.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
        statement.whilePart.statementList.children.forEach { it.delete() }
        statement.whilePart.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
        add(codeHintWhileStatement)
      }

      is PyForStatement -> {
        val codeHintForStatement = statement.forPart
        codeHintForStatement.statementList.deleteChildRange(
          codeHintForStatement.statementList.firstChild,
          codeHintForStatement.statementList.lastChild
        )
        codeHintForStatement.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
        add(codeHintForStatement)
      }

      is PyIfStatement -> {
        val codeHintIfStatement = statement.ifPart
        codeHintIfStatement.statementList.deleteChildRange(
          codeHintIfStatement.statementList.firstChild,
          codeHintIfStatement.statementList.lastChild
        )
        codeHintIfStatement.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
        add(codeHintIfStatement)
      }

      else -> add(statement)
    }
  }

  private fun PyStatement.replaceIfNeeded(codeHintStatement: PyIfStatement): Boolean { // todo: elifParts, elsePart
    if (this !is PyIfStatement) { // If current statement is not `if`, replace with `if` part and `pass` statement as a body
      val codeHintIfPart = codeHintStatement.ifPart
      codeHintIfPart.statementList.deleteChildRange(codeHintIfPart.statementList.firstChild, codeHintIfPart.statementList.lastChild)
      codeHintIfPart.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
      replace(codeHintIfPart)
      return true
    }
    if (replaceIfNeeded(ifPart, codeHintStatement.ifPart)) {
      return true
    }
    return false
  }

  private fun PyStatement.replaceIfNeeded(codeHintStatement: PyForStatement): Boolean { // todo: elsePart
    if (this !is PyForStatement) { // If current statement is not `for`, replace with the loop and `pass` statement as a body
      val codeHintForPart = codeHintStatement.forPart
      codeHintForPart.statementList.deleteChildRange(codeHintForPart.statementList.firstChild, codeHintForPart.statementList.lastChild)
      codeHintForPart.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
      replace(codeHintForPart)
      return true
    }
    if (replaceIfNeeded(forPart, codeHintStatement.forPart)) {
      return true
    }
    return false
  }

  private fun PyStatement.replaceIfNeeded(codeHintWhileStatement: PyWhileStatement): Boolean { // todo: elsePart
    if (this !is PyWhileStatement) { // If current statement is not `while`, replace with the loop and `pass` statement as a body
      val codeHintWhilePart = codeHintWhileStatement.whilePart
      codeHintWhilePart.statementList.deleteChildRange(
        codeHintWhilePart.statementList.firstChild,
        codeHintWhilePart.statementList.lastChild
      )
      codeHintWhilePart.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
      replace(codeHintWhilePart)
      return true
    }
    if (replaceIfNeeded(whilePart, codeHintWhileStatement.whilePart)) {
      return true
    }
    return false
  }

  private fun replaceIfNeeded(
    currentPyStatementPart: PyStatementPart,
    codeHintPyStatementPart: PyStatementPart,
  ): Boolean {
    if (currentPyStatementPart.text == codeHintPyStatementPart.text) return false

    when (currentPyStatementPart) {
      is PyWhilePart, is PyIfPart -> {
        val currentBinaryExpression = currentPyStatementPart.children.firstOrNull { it is PyBinaryExpression } ?: return false
        val codeHintBinaryExpression = codeHintPyStatementPart.children.firstOrNull { it is PyBinaryExpression } ?: return false
        if (currentBinaryExpression.text != codeHintBinaryExpression.text) {
          currentBinaryExpression.replace(codeHintBinaryExpression)
          return true
        }
      }

      is PyForPart -> {
        val currentForPart = currentPyStatementPart.copy() as PyForPart
        currentForPart.lastChild.delete()
        val codeHintForPart = codeHintPyStatementPart.copy() as PyForPart
        codeHintForPart.lastChild.delete()
        if (currentForPart.text != codeHintForPart.text) {
          currentForPart.replace(codeHintForPart)
          codeHintForPart.add(currentPyStatementPart.statementList)
          currentPyStatementPart.replace(codeHintForPart)
          return true
        }
      }
    }

    val currentStatements = currentPyStatementPart.statementList.statements
    val codeHintStatements = codeHintPyStatementPart.statementList.statements
    currentStatements.zip(codeHintStatements).forEach { (currentStatement, codeHintStatement) ->
      if (currentStatement.text != codeHintStatement.text) {
        currentStatement.replace(codeHintStatement)
        return true
      }
    }
    if (currentStatements.size < codeHintStatements.size) {
      currentPyStatementPart.statementList.add(codeHintStatements[currentStatements.size])
      return true
    }
    return false
  }

  private fun PyParameterList.replaceIfNeeded(codeHintParameterList: PyParameterList): Boolean {
    if (text != codeHintParameterList.text) {
      replace(codeHintParameterList)
      return true
    }
    return false
  }

  private fun PyAnnotation?.replaceIfNeeded(codeHintAnnotation: PyAnnotation?): Boolean {
    val currentAnnotation = this ?: return false
    if (codeHintAnnotation == null) {
      currentAnnotation.delete()
      return true
    }
    if (currentAnnotation.text != codeHintAnnotation.text) {
      currentAnnotation.replace(codeHintAnnotation)
      return true
    }
    return false
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