package com.jetbrains.edu.cpp.radler.checker

import com.intellij.clion.radler.core.symbols.RadMainPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cpp.checker.CppRunConfigurationHelper

class RadlerCppRunConfigurationHelper : CppRunConfigurationHelper {
  override fun prepareEntryPointForRunConfiguration(project: Project, entryPoint: PsiElement): PsiElement? {
    val virtualFile = entryPoint.containingFile.virtualFile ?: return null
    return RadMainPsiElement(project, virtualFile, entryPoint.textRange)
  }
}