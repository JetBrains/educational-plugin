package com.jetbrains.edu.java

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.handlers.EduMoveDelegate
import com.jetbrains.edu.learning.handlers.EduRenameHandler
import com.jetbrains.edu.learning.handlers.isRenameAndMoveForbidden

class JMoveRenameHandler : EduMoveDelegate(), EduRenameHandler {
  override fun canMove(dataContext: DataContext?): Boolean {
    return isAvailable(CommonDataKeys.PROJECT.getData(dataContext!!), CommonDataKeys.PSI_ELEMENT.getData(dataContext))
  }

  override fun canMove(elements: Array<PsiElement>, targetContainer: PsiElement?): Boolean {
    return if (elements.size == 1) isAvailable(elements[0].project, elements[0]) else false
  }

  override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
    return isAvailable(CommonDataKeys.PROJECT.getData(dataContext), CommonDataKeys.PSI_ELEMENT.getData(dataContext))
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

  private fun isAvailable(project: Project?, element: PsiElement?): Boolean {
    if (element == null || project == null) return false
    if (!EduUtils.isStudentProject(project)) return false
    val course = StudyTaskManager.getInstance(project).course ?: error("Project ${project.name} should have course instance")
    val adjustedElement = adjustElement(element) ?: return false
    return isRenameAndMoveForbidden(project, course, adjustedElement)
  }

  private fun adjustElement(element: PsiElement): PsiElement? {
    val psiClass = element as? PsiClass ?: return null
    val containingFile = psiClass.containingFile
    val virtualFile = containingFile.virtualFile ?: return null
    val taskFile = EduUtils.getTaskFile(element.project, virtualFile) ?: return null
    return if (!taskFile.isLearnerCreated && virtualFile.nameWithoutExtension == psiClass.name) containingFile else null
  }
}
