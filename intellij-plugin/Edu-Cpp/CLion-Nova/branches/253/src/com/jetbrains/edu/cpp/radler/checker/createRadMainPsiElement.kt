package com.jetbrains.edu.cpp.radler.com.jetbrains.edu.cpp.radler.checker

import com.intellij.clion.radler.core.symbols.RadMainPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

fun createRadMainPsiElement(project: Project, entryPoint: PsiElement): PsiElement? {
  return RadMainPsiElement(entryPoint.containingFile, entryPoint.textRange)
}