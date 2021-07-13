package com.jetbrains.edu.java.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.handlers.EduMoveDelegate
import com.jetbrains.edu.learning.handlers.isMoveForbidden

class JMoveHandler : EduMoveDelegate() {
  override fun canMove(dataContext: DataContext): Boolean {
    return isMoveAvailable(CommonDataKeys.PROJECT.getData(dataContext),
                           CommonDataKeys.PSI_ELEMENT.getData(dataContext),
                           LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext))
  }

  override fun canMove(elements: Array<PsiElement>, targetContainer: PsiElement?): Boolean {
    return if (elements.size == 1) isMoveAvailable(elements[0].project, elements[0], targetContainer) else false
  }

  private fun isMoveAvailable(project: Project?, source: PsiElement?, target: PsiElement?): Boolean {
    return isRefactoringAvailable(project, source) { p, element ->
      isMoveForbidden(p, element, target)
    }
  }

  private fun isRefactoringAvailable(project: Project?, element: PsiElement?, isAvailable: (Project, PsiElement) -> Boolean): Boolean {
    if (element == null || project == null) return false
    val adjustedElement = adjustElement(element) ?: return false
    return isAvailable(project, adjustedElement)
  }
}
