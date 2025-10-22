package com.jetbrains.edu.cpp.radler.checker

import com.intellij.clion.radler.core.symbols.RadMainPsiElement
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cpp.checker.CppRunConfigurationHelper

// BACKCOMPAT: 2025.2. Inline it
class RadlerCppRunConfigurationHelper : CppRunConfigurationHelper {
  override fun prepareEntryPointForRunConfiguration(entryPoint: PsiElement): PsiElement =
    RadMainPsiElement(entryPoint.containingFile, entryPoint.textRange)
}