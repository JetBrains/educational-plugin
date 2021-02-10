package com.jetbrains.edu.go.checker

import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.edu.go.codeforces.GoCodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration

// We need factory in 202 branch
fun createGoCodeforcesConfiguration(
  project: Project,
  @Suppress("UNUSED_PARAMETER") factory: ConfigurationFactory
): CodeforcesRunConfiguration {
  return GoCodeforcesRunConfiguration(project)
}

fun getGoInputRedirectOptions(configuration: GoCodeforcesRunConfiguration): InputRedirectAware.InputRedirectOptions {
  return configuration
}
