package com.jetbrains.edu.aiHints.python.impl

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyForStatement
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementPart
import com.jetbrains.python.psi.PyWhileStatement

fun PyStatementList.firstDifferentStatement(another: PyStatementList): Int? {
  val zipped = statements.zip(another.statements)
  for (i in zipped.indices) {
    if (zipped[i].first::class != zipped[i].second::class) return i
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

internal fun PsiElement.compareAndReplaceIfNeeded(other: PsiElement): Boolean {
  if (!compareNormalized(other)) {
    replace(other)
    return true
  }
  return false
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