package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Forbids renaming of study items and task files for courses in study mode
 */
class EduCourseItemRenameHandler : EduRenameHandler {
  override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
    return isRenameForbidden(dataContext)
  }

  override fun isRenaming(dataContext: DataContext): Boolean {
    return isAvailableOnDataContext(dataContext)
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) {
    Messages.showInfoMessage("This rename operation can break the course", "Invalid Rename Operation")
  }

  override fun invoke(project: Project, elements: Array<PsiElement>, dataContext: DataContext) {
    invoke(project, null, null, dataContext)
  }
}
