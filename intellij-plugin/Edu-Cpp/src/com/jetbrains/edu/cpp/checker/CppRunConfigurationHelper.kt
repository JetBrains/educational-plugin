package com.jetbrains.edu.cpp.checker

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

interface CppRunConfigurationHelper {
  /**
   * Process given entryPoint into the psi element suitable to create the run configuration.
   *
   * In CLion-Nova, to create a run configuration using an entry point element, you need to use a specific `RadMainPsiElement`.
   */
  fun prepareEntryPointForRunConfiguration(project: Project, entryPoint: PsiElement): PsiElement?

  companion object {
    private val EP_NAME: ExtensionPointName<CppRunConfigurationHelper> = ExtensionPointName.create("Educational.cppRunConfigurationHelper")

    // It is assumed at any given time that only one extension point will be available
    fun getInstance(): CppRunConfigurationHelper? = EP_NAME.extensionList.singleOrNull()
  }
}