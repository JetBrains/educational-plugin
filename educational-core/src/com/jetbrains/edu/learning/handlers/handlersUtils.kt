@file:JvmName("HandlersUtils")

package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.*

private fun isRefactoringForbidden(project: Project?, element: PsiElement?): Boolean {
  if (project == null || element == null) return false
  val course = project.course ?: return false
  if (!course.isStudy) return false
  return when (element) {
    is PsiFile -> {
      // TODO: allow changing user created non-task files EDU-2556
      val taskFile = element.originalFile.virtualFile.getTaskFile(project)
      taskFile == null
    }
    is PsiDirectory -> {
      val dir = element.virtualFile
      dir.getStudyItem(project) != null
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
    if (project == null) return false
    val sourceTaskDir = element.originalFile.virtualFile.getTaskDir(project) ?: return false
    val targetDir = (target as? PsiDirectory)?.virtualFile ?: return false
    val targetTaskDir = if (targetDir.isTaskDirectory(project)) {
      targetDir
    } else {
      targetDir.getTaskDir(project)
    }

    if (sourceTaskDir != targetTaskDir) return true
  }
  return false
}

fun isMoveForbidden(dataContext: DataContext): Boolean = isMoveForbidden(
  CommonDataKeys.PROJECT.getData(dataContext),
  CommonDataKeys.PSI_ELEMENT.getData(dataContext),
  LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext)
)
