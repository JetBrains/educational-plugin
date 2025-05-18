package com.jetbrains.edu.aiHints.python.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.api.FunctionDiffReducer
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.PyStatementWithElse

private const val SMALL_FUNCTION_SIZE: Int = 3

object PyFunctionDiffReducer : FunctionDiffReducer {
  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement): PsiElement {
    val project = runReadAction { modifiedFunction.project }
    val codeHint = modifiedFunction.asSafely<PyFunction>() ?: return modifiedFunction
    if (function != null) {
      val currentFunction = function.asSafely<PyFunction>() ?: return modifiedFunction
      val codeHintFunctionSize = runReadAction { codeHint.text.lines().size }
      if (codeHintFunctionSize <= SMALL_FUNCTION_SIZE) return codeHint
      return runWriteCommandAction(project) {
        syncTree(project, currentFunction, codeHint)
        currentFunction
      }
    }
    reduceNewStatement(project, codeHint)
    return codeHint
  }

  private fun syncTree(project: Project, current: PsiElement, codeHint: PsiElement): Boolean {
    when {
      current is PyFunction && codeHint is PyFunction -> {
        return current.parameterList.deleteOrSwapWith(project, codeHint.parameterList)
               || current.annotation.deleteOrSwapWith(project, codeHint.annotation)
               || syncTree(project, current.statementList, codeHint.statementList)
      }

      current is PyExpressionStatement && codeHint is PyExpressionStatement ||
      current is PyAssignmentStatement && codeHint is PyAssignmentStatement ||
      current is PyReturnStatement && codeHint is PyReturnStatement -> {
        return current.compareAndReplaceIfNeeded(codeHint)
      }

      current is PyWhileStatement && codeHint is PyWhileStatement -> {
        return syncTree(project, current.whilePart, codeHint.whilePart)
               || syncElseParts(project, current, codeHint)
      }

      current is PyWhilePart && codeHint is PyWhilePart -> {
        return current.condition.deleteOrSwapWith(project, codeHint.condition)
               || syncTree(project, current.statementList, codeHint.statementList)
      }

      current is PyForStatement && codeHint is PyForStatement -> {
        return syncTree(project, current.forPart, codeHint.forPart)
               || syncElseParts(project, current, codeHint)
      }

      current is PyForPart && codeHint is PyForPart -> {
        return current.target.deleteOrSwapWith(project, codeHint.target)
               || current.source.deleteOrSwapWith(project, codeHint.source)
               || syncTree(project, current.statementList, codeHint.statementList)
      }

      current is PyIfStatement && codeHint is PyIfStatement -> {
        return syncTree(project, current.ifPart, codeHint.ifPart)
               || current.syncElifParts(codeHint)
               || syncElseParts(project, current, codeHint)
      }

      current is PyIfPart && codeHint is PyIfPart -> {
        return current.condition.deleteOrSwapWith(project, codeHint.condition)
               || syncTree(project, current.statementList, codeHint.statementList)
      }

      current is PyIfPartElif && codeHint is PyIfPartElif -> {
        return current.condition.deleteOrSwapWith(project, codeHint.condition)
               || syncTree(project, current.statementList, codeHint.statementList)
      }

      current is PyElsePart && codeHint is PyElsePart -> {
        return syncTree(project, current.statementList, codeHint.statementList)
      }

      current is PyStatementList && codeHint is PyStatementList -> {
        val currentStatements = current.statements
        val codeHintStatements = codeHint.statements

        // If the only existing statement is a `pass` statement, replace it with the first statement from CodeHint
        val passStatement = currentStatements.singleOrNull().asSafely<PyPassStatement>()
        if (passStatement != null) {
          val newStatement = codeHintStatements.firstOrNull()?.copy() as? PyStatement ?: return false
          reduceNewStatement(project, newStatement)
          current.statements.firstOrNull()?.replace(newStatement) ?: return false
          return true
        }

        if (currentStatements.size < codeHintStatements.size) {
          val indexOfDifference = codeHint.firstDifferentStatement(current)

          if (indexOfDifference != null) {
            val newStatement = codeHintStatements.elementAtOrNull(indexOfDifference)?.copy() ?: return false
            reduceNewStatement(project, newStatement)
            val anchor = currentStatements.elementAtOrNull(indexOfDifference)
            runWriteCommandAction(project) {
              current.addBefore(newStatement, anchor)
            }
            return true
          }
          else {
            // All statements from current are already in the CodeHint, need to add a next statement from the code hint
            val newStatement = codeHintStatements.elementAtOrNull(currentStatements.size)?.copy() ?: return false
            reduceNewStatement(project, newStatement)
            runWriteCommandAction(project) {
              current.add(newStatement)
            }
            return true
          }
        }

        val statementsToSync = current.statements.zip(codeHint.statements)
        for ((currentStatement, hintStatement) in statementsToSync) {
          if (!currentStatement.compareNormalized(hintStatement)) {
            return syncTree(project, currentStatement, hintStatement)
          }
        }
        return false
      }

      current is PyStatement && codeHint is PyStatement -> {
        reduceNewStatement(project, codeHint)
        runWriteCommandAction(project) {
          current.replace(codeHint)
        }
        return true
      }
    }
    return false // No modification made
  }

  private fun reduceNewStatement(project: Project, statement: PsiElement): PsiElement {
    when (statement) {
      is PyFunction -> {
        val functionSize = runReadAction { statement.text.lines().size }
        if (functionSize <= SMALL_FUNCTION_SIZE) return statement
        runWriteCommandAction(project) {
          statement.statementList.children.forEach { it.delete() }
        }
      }

      is PyWhileStatement, is PyForStatement -> {
        runWriteCommandAction(project) {
          statement.mainPart.statementList.children.forEach { it.delete() }
          statement.elsePart?.statementList?.children?.forEach { it.delete() }
        }
      }

      is PyIfStatement -> {
        runWriteCommandAction(project) {
          statement.ifPart.statementList.children.forEach { it.delete() }
          statement.elifParts.forEach { it.delete() }
          statement.elsePart?.statementList?.children?.forEach { it.delete() }
        }
      }

      is PyIfPartElif -> {
        runWriteCommandAction(project) {
          statement.statementList.children.forEach { it.delete() }
        }
      }
    }

    return statement
  }

  private fun syncElseParts(project: Project, current: PyStatementWithElse, codeHint: PyStatementWithElse): Boolean {
    val currentElsePart = current.elsePart
    val codeHintElsePart = codeHint.elsePart
    if (currentElsePart == null && codeHintElsePart != null) {
      val elsePartToAdd = codeHintElsePart.copy() as PyElsePart
      elsePartToAdd.statementList.statements.forEach { it.delete() }
      current.add(elsePartToAdd)
    } else if (currentElsePart != null && codeHintElsePart == null) {
      currentElsePart.delete()
    } else if (currentElsePart != null && codeHintElsePart != null) {
      return syncTree(project, currentElsePart, codeHintElsePart)
    }
    return false
  }

  private fun PyIfStatement.syncElifParts(codeHintStatement: PyIfStatement): Boolean {
    val currentElifParts = elifParts
    val codeHintElifParts = codeHintStatement.elifParts

    if (currentElifParts.isNotEmpty() && codeHintElifParts.isNotEmpty()) {
      // Insert next `elif` if possible
      if (currentElifParts.size < codeHintElifParts.size) {
        val reducedElifPart = codeHintElifParts.elementAtOrNull(currentElifParts.size)?.copy() ?: return false
        reduceNewStatement(project, reducedElifPart)
        addBefore(reducedElifPart, elsePart)
        return true
      }
      for ((currentElifPart, codeHintElifPart) in currentElifParts.zip(codeHintElifParts)) {
        if (currentElifPart.compareNormalized(codeHintElifPart)) continue
        return syncTree(project, currentElifPart, codeHintElifPart)
      }
    }
    else if (currentElifParts.isEmpty() && codeHintElifParts.isNotEmpty()) {
      // Add the first elif part (reduced) from CodeHint
      val reducedElifPart = codeHintElifParts.firstOrNull()?.copy() ?: return false
      reduceNewStatement(project, reducedElifPart)
      addBefore(reducedElifPart, elsePart)
      return true
    }
    else if (currentElifParts.isNotEmpty()) { // CodeHint's elif parts are empty
      // Removing all `elif` parts
      runWriteCommandAction(project) {
        currentElifParts.forEach { it.delete() }
      }
      return true
    } // else: both are empty, do nothing

    return false
  }
}