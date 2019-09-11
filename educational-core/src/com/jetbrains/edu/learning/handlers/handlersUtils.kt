@file:JvmName("HandlersUtils")

package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.EduUtils.getStudyItem
import com.jetbrains.edu.learning.EduUtils.getTaskFile
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course

fun isRenameAndMoveForbidden(project: Project, course: Course, element: PsiElement): Boolean {
  if (!course.isStudy) return false
  return when (element) {
    is PsiFile -> {
      // TODO: allow changing user created non-task files EDU-2556
      val taskFile = getTaskFile(project, element.originalFile.virtualFile) ?: return true
      !taskFile.isLearnerCreated
    }
    is PsiDirectory -> {
      val dir = element.virtualFile
      getStudyItem(project, dir) != null
    }
    else -> false
  }
}

fun isRenameAndMoveForbidden(dataContext: DataContext): Boolean {
  val project = CommonDataKeys.PROJECT.getData(dataContext)
  val element = CommonDataKeys.PSI_ELEMENT.getData(dataContext)
  if (element == null || project == null) return false
  val course = project.course ?: return false
  return course.isStudy && isRenameAndMoveForbidden(project, course, element)
}
