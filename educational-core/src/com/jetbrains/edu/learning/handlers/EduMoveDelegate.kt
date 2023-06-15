package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveHandlerDelegate
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.messages.EduCoreBundle.message

open class EduMoveDelegate : MoveHandlerDelegate() {
  override fun canMove(dataContext: DataContext): Boolean {
    return isMoveForbidden(dataContext)
  }

  override fun canMove(elements: Array<PsiElement>, targetContainer: PsiElement?, reference: PsiReference?): Boolean {
    return if (elements.size == 1) {
      isMoveForbidden(elements[0].project, elements[0], targetContainer)
    }
    else false
  }

  override fun isValidTarget(psiElement: PsiElement?, sources: Array<PsiElement>): Boolean = true

  override fun doMove(
    project: Project,
    elements: Array<PsiElement>,
    targetContainer: PsiElement?,
    callback: MoveCallback?
  ) {
    Messages.showInfoMessage(message("messages.move.operation.description"), message("messages.move.invalid"))
  }

  override fun tryToMove(
    element: PsiElement,
    project: Project,
    dataContext: DataContext,
    reference: PsiReference?,
    editor: Editor
  ): Boolean {
    return project.isEduProject()
  }
}
