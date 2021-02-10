package com.jetbrains.edu.rust.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.rust.codeforces.RsCodeforcesRunConfiguration

fun createRsCodeforcesConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration {
  return RsCodeforcesRunConfiguration(project, factory)
}
