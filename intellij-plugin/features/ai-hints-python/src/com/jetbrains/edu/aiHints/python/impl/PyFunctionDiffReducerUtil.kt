package com.jetbrains.edu.aiHints.python.impl

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyForStatement
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyPassStatement
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementListContainer
import com.jetbrains.python.psi.PyStatementPart
import com.jetbrains.python.psi.PyWhileStatement

/**
 * Compare given [PyStatementListContainer] (e.g. [com.jetbrains.python.psi.PyFunction]) with [another] statement by statement
 *
 * @return The first pair of [PyStatement]s that differs
 */
fun PyStatementListContainer.firstDifferentStatement(another: PyStatementListContainer): Pair<PyStatement, PyStatement>? =
  statementList.statements.zip(another.statementList.statements).firstOrNull {
    !it.first.compareNormalized(it.second)
  }

fun PyStatementListContainer.findNextFrom(other: PyStatementListContainer): PyStatement? {
  val currentStatementListSize = statementList.statements.size
  if (currentStatementListSize < other.statementList.statements.size) {
    return other.statementList.statements.getOrNull(currentStatementListSize)
  }
  return null
}

val PyStatement.mainPart: PyStatementPart
  get() = when (this) {
    is PyIfStatement -> ifPart
    is PyForStatement -> forPart
    is PyWhileStatement -> whilePart
    else -> error("Unexpected statement type: ${this::class.java}")
  }

val PyStatement.elsePart: PyStatementPart?
  get() = when (this) {
    is PyIfStatement -> elsePart
    is PyForStatement -> elsePart
    is PyWhileStatement -> elsePart
    else -> null
  }

/**
 * Reduces the given [PyStatementPart] and adds it to the [PsiElement] it's been called on
 * by calling [PsiElement.add] or [PsiElement.addBefore] if the [anchor] is provided.
 *
 * Reducing means replacing all children of the [PyStatementPart]'s [PyStatementList] with the [PyPassStatement].
 */
internal fun PsiElement.reduceAndAdd(pyStatementPart: PyStatementPart, anchor: PsiElement? = null) {
  runWriteCommandAction(project) {
    pyStatementPart.statementList.deleteChildRange(
      pyStatementPart.statementList.firstChild,
      pyStatementPart.statementList.lastChild
    )
    pyStatementPart.statementList.add(PyElementGenerator.getInstance(project).createPassStatement())
    if (anchor != null) {
      addBefore(pyStatementPart, anchor)
    }
    else {
      add(pyStatementPart)
    }
  }
}

internal fun PsiElement.replace(project: Project, element: PsiElement) = runWriteCommandAction(project) {
  replace(element)
}

internal fun <R> runWriteCommandAction(project: Project, action: () -> R): R {
  var result: R? = null
  WriteCommandAction.runWriteCommandAction(project, null, null, {
    result = action()
  })
  @Suppress("UNCHECKED_CAST")
  return result as R
}

internal fun PsiElement?.deleteOrSwapWith(project: Project, second: PsiElement?): Boolean {
  if (this == null) return false
  if (second == null) {
    runWriteCommandAction(project) {
      delete()
    }
    return true
  }
  if (!compareNormalized(second)) {
    replace(project, second)
    return true
  }
  return false
}

/**
 * Compares [PsiElement.getText] of two [PsiElement]. This function is used to decide if the modification is needed.
 */
internal fun PsiElement.compareNormalized(psiElement: PsiElement): Boolean {
  val currentTextNormalized = text.replace(" ", "")
  val anotherTextNormalized = psiElement.text.replace(" ", "")
  return currentTextNormalized == anotherTextNormalized
}