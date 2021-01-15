package com.jetbrains.edu.kotlin.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.checker.GradleCodeExecutor
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.InvalidCodeforcesRunConfiguration

class KtCodeExecutor : GradleCodeExecutor() {
  override fun createCodeforcesConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration {
    // We have InputRedirectOptions only from 203 branch
    return InvalidCodeforcesRunConfiguration(project, factory)
  }
}
