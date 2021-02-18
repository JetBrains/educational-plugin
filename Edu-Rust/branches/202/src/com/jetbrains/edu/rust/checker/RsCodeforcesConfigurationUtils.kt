package com.jetbrains.edu.rust.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.InvalidCodeforcesRunConfiguration

fun createRsCodeforcesConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration {
  // We have InputRedirectOptions only from 203 branch
  return InvalidCodeforcesRunConfiguration(project, factory)
}
