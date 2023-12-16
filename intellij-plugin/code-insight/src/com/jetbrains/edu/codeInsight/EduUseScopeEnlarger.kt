package com.jetbrains.edu.codeInsight

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.canBelongToCourse
import com.jetbrains.edu.learning.courseDir

class EduUseScopeEnlarger : UseScopeEnlarger() {

  override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
    val project = element.project
    if (!project.isEduProject()) return null
    if (element !is PsiFileSystemItem) return null

    val virtualFile = element.virtualFile ?: return null
    if (!virtualFile.canBelongToCourse(project)) return null
    return GlobalSearchScopes.directoryScope(project, project.courseDir, true)
  }
}