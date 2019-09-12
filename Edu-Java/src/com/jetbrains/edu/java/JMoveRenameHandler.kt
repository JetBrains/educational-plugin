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
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.handlers.EduMoveDelegate
import com.jetbrains.edu.learning.handlers.EduRenameHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class JMoveRenameHandler : EduMoveDelegate(), EduRenameHandler {
  override fun canMove(dataContext: DataContext?): Boolean {
    return canRenameOrMove(CommonDataKeys.PROJECT.getData(dataContext!!), CommonDataKeys.PSI_ELEMENT.getData(dataContext))
  }

  override fun canMove(elements: Array<PsiElement>, targetContainer: PsiElement?): Boolean {
    return if (elements.size == 1) {
      canRenameOrMove(elements[0].project, elements[0])
    }
    else false
  }

  override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
    return canRenameOrMove(CommonDataKeys.PROJECT.getData(dataContext), CommonDataKeys.PSI_ELEMENT.getData(dataContext))
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

  private fun canRenameOrMove(project: Project?, element: PsiElement?): Boolean {
    if (element == null || project == null) {
      return false
    }
    val course = StudyTaskManager.getInstance(project)!!.course
    if (!EduUtils.isStudentProject(project)) {
      return false
    }

    assert(course != null)
    val elementToMove = getElementToMove(element, course!!)
    return !EduUtils.isRenameAndMoveForbidden(project, course, elementToMove)
  }

  private fun getElementToMove(element: PsiElement, course: Course): PsiElement {
    // prevent class renaming in hyperskill? courses
    if (course is HyperskillCourse && element is PsiClass) {
      val fileName = element.getContainingFile().name
      val dotIndex = fileName.lastIndexOf('.')
      val fileNameWithoutExtension = if (dotIndex >= 0) fileName.substring(0, dotIndex) else fileName
      val className = element.name
      if (fileNameWithoutExtension == className) {
        return element.getContainingFile()
      }
    }

    return element
  }
}
