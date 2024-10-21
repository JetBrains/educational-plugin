package com.jetbrains.edu.cpp.radler.checker

import com.intellij.psi.PsiElement
import com.jetbrains.cidr.radler.symbols.RadMainPsiElement
import com.jetbrains.edu.cpp.checker.CppRunConfigurationHelper

class RadlerCppRunConfigurationHelper : CppRunConfigurationHelper {
  override fun prepareEntryPointForRunConfiguration(entryPoint: PsiElement): PsiElement = RadMainPsiElement(entryPoint.containingFile, entryPoint.textRange)
}