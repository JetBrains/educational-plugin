package com.jetbrains.edu.aiHints.python.impl

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyPassStatement
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementPart

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

internal fun <R> runWriteCommandAction(project: Project, action: () -> R): R {
  var result: R? = null
  WriteCommandAction.runWriteCommandAction(project, null, null, {
    result = action()
  })
  @Suppress("UNCHECKED_CAST")
  return result as R
}