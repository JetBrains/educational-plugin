package com.jetbrains.edu.kotlin.checker

import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.gradle.checker.GradleCodeExecutor
import com.jetbrains.edu.kotlin.codeforces.KtCodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration

class KtCodeExecutor : GradleCodeExecutor() {
  override fun createRedirectInputConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration? {
    return KtCodeforcesRunConfiguration(JavaRunConfigurationModule(project, true), factory)
  }

  override fun setInputRedirectFile(inputFile: VirtualFile, configuration: RunConfiguration) {
    val options = (configuration as InputRedirectAware).inputRedirectOptions
    options.isRedirectInput = true
    options.redirectInputPath = inputFile.path
  }
}
