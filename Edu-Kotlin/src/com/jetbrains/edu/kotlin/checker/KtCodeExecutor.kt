package com.jetbrains.edu.kotlin.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.checker.GradleCodeExecutor
import com.jetbrains.edu.kotlin.codeforces.KtCodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration

class KtCodeExecutor : GradleCodeExecutor() {
  override fun createRedirectInputConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration? {
    return KtCodeforcesRunConfiguration(JavaRunConfigurationModule(project, true), factory)
  }
}
