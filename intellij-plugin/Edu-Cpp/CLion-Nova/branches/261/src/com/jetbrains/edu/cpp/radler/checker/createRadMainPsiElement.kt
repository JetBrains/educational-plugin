package com.jetbrains.edu.cpp.radler.com.jetbrains.edu.cpp.radler.checker

import com.intellij.clion.radler.core.symbols.RadMainPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

// BACKCOMPAT 2025.3. Inline it
fun createRadMainPsiElement(project: Project, entryPoint: PsiElement): PsiElement? {
  val virtualFile = entryPoint.containingFile.virtualFile ?: return null
  return RadMainPsiElement(project, virtualFile, entryPoint.textRange)
}