package com.jetbrains.edu.cpp.radler.checker

import com.intellij.clion.radler.core.symbols.RadMainPsiElement
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cpp.checker.CppRunConfigurationHelper

class RadlerCppRunConfigurationHelper : CppRunConfigurationHelper {
  override fun prepareEntryPointForRunConfiguration(entryPoint: PsiElement): PsiElement {
    return RadMainPsiElement(entryPoint.containingFile, entryPoint.textRange)
  }
}