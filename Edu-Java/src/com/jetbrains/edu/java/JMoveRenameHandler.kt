package com.jetbrains.edu.java

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.handlers.EduMoveDelegate
import com.jetbrains.edu.learning.handlers.EduRenameHandler
import com.jetbrains.edu.learning.handlers.isMoveForbidden
import com.jetbrains.edu.learning.handlers.isRenameForbidden

class JMoveRenameHandler : EduMoveDelegate(), EduRenameHandler {
  override fun canMove(dataContext: DataContext): Boolean {
    return isMoveAvailable(CommonDataKeys.PROJECT.getData(dataContext),
                           CommonDataKeys.PSI_ELEMENT.getData(dataContext),
                           LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext))
  }

  override fun canMove(elements: Array<PsiElement>, targetContainer: PsiElement?): Boolean {
    return if (elements.size == 1) isMoveAvailable(elements[0].project, elements[0], targetContainer) else false
  }

  override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
    return isRenameAvailable(CommonDataKeys.PROJECT.getData(dataContext), CommonDataKeys.PSI_ELEMENT.getData(dataContext))
  }

  override fun isRenaming(dataContext: DataContext): Boolean {
    return isAvailableOnDataContext(dataContext)
  }

  override fun invoke(project: Project, editor: Editor?, psiFile: PsiFile?, dataContext: DataContext) {
    Messages.showInfoMessage("This rename operation can break the course", "Invalid Rename Operation")
  }

  override fun invoke(project: Project, psiElements: Array<PsiElement>, dataContext: DataContext) {
    invoke(project, null, null, dataContext)
  }

  private fun isRenameAvailable(project: Project?, element: PsiElement?): Boolean {
    return isRefactoringAvailable(project, element, ::isRenameForbidden)
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

  private fun adjustElement(element: PsiElement): PsiElement? {
    val psiClass = element as? PsiClass ?: return null
    val containingFile = psiClass.containingFile
    val virtualFile = containingFile.virtualFile ?: return null
    val taskFile = EduUtils.getTaskFile(element.project, virtualFile) ?: return null
    return if (!taskFile.isLearnerCreated && virtualFile.nameWithoutExtension == psiClass.name) containingFile else null
  }
}
