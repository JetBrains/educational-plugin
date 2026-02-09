package com.jetbrains.edu.cpp.radler.checker

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cpp.checker.CppRunConfigurationHelper
import com.jetbrains.edu.cpp.radler.com.jetbrains.edu.cpp.radler.checker.createRadMainPsiElement

class RadlerCppRunConfigurationHelper : CppRunConfigurationHelper {
  override fun prepareEntryPointForRunConfiguration(project: Project, entryPoint: PsiElement): PsiElement? {
    return createRadMainPsiElement(project, entryPoint)
  }
}