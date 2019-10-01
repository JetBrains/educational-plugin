@file:JvmName("HandlersUtils")

package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.EduUtils.getStudyItem
import com.jetbrains.edu.learning.EduUtils.getTaskFile
import com.jetbrains.edu.learning.course

private fun isRefactoringForbidden(project: Project?, element: PsiElement?): Boolean {
  if (project == null || element == null) return false
  val course = project.course ?: return false
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

fun isRenameForbidden(project: Project?, element: PsiElement?): Boolean {
  return isRefactoringForbidden(project, element)
}

fun isMoveForbidden(project: Project?, element: PsiElement?, target: PsiElement?): Boolean {
  if (isRefactoringForbidden(project, element)) return true
  if (element is PsiFile) {
    val course = project?.course ?: return false
    val sourceTaskDir = EduUtils.getTaskDir(course, element.originalFile.virtualFile) ?: return false
    val targetDir = (target as? PsiDirectory)?.virtualFile ?: return false
    val targetTaskDir = if (EduUtils.isTaskDirectory(project, targetDir)) {
      targetDir
    } else {
      EduUtils.getTaskDir(course, targetDir)
    }

    if (sourceTaskDir != targetTaskDir) return true
  }
  return false
}

fun isRenameForbidden(dataContext: DataContext): Boolean = isRenameForbidden(
  CommonDataKeys.PROJECT.getData(dataContext),
  CommonDataKeys.PSI_ELEMENT.getData(dataContext)
)

fun isMoveForbidden(dataContext: DataContext): Boolean = isMoveForbidden(
  CommonDataKeys.PROJECT.getData(dataContext),
  CommonDataKeys.PSI_ELEMENT.getData(dataContext),
  LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext)
)
