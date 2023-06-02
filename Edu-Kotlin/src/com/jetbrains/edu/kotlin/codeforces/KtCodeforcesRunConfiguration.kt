package com.jetbrains.edu.kotlin.codeforces

import com.intellij.execution.application.JvmMainMethodRunConfigurationOptions
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinMainFunctionDetector
import org.jetbrains.kotlin.idea.base.codeInsight.findMainOwner
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer.Companion.getMainClassJvmName

class KtCodeforcesRunConfiguration(runConfigurationModule: JavaRunConfigurationModule, factory: ConfigurationFactory) :
  KotlinRunConfiguration(CodeforcesRunConfigurationType.CONFIGURATION_ID, runConfigurationModule, factory),
  CodeforcesRunConfiguration {
  @Suppress("UnstableApiUsage")
  override fun setExecutableFile(file: VirtualFile) {
    setModule(ModuleUtilCore.findModuleForFile(file, project))
    val element = PsiManager.getInstance(project).findFile(file)
                  ?: throw IllegalStateException("Unable to find psiElement for " + file.path)
    val container = KotlinMainFunctionDetector.getInstance().findMainOwner(element)
                    ?: throw IllegalStateException("Unable to set executable file for " + file.path)
    val name = getMainClassJvmName(container)
    runClass = name
  }

  override fun getDefaultOptionsClass(): Class<out ModuleBasedConfigurationOptions> =
    JvmMainMethodRunConfigurationOptions::class.java
}
