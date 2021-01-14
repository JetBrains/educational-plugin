package com.jetbrains.edu.codeInsight

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.*
import com.jetbrains.edu.learning.*

class EduUseScopeEnlarger : UseScopeEnlarger() {

  override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
    val project = element.project
    if (!EduUtils.isEduProject(project)) return null
    if (element !is PsiFileSystemItem) return null

    if (!element.virtualFile.canBelongToCourse(project)) return null
    return GlobalSearchScopes.directoryScope(project, project.courseDir, true)
  }
}