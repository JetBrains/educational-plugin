package com.jetbrains.edu.cpp.classic.checker

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cpp.checker.CppRunConfigurationHelper

class ClassicCppRunConfigurationHelper : CppRunConfigurationHelper {
  override fun prepareEntryPointForRunConfiguration(entryPoint: PsiElement): PsiElement = entryPoint
}