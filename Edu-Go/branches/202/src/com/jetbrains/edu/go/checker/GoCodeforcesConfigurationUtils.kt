package com.jetbrains.edu.go.checker

import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.edu.go.codeforces.GoCodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.InvalidCodeforcesRunConfiguration
import java.lang.UnsupportedOperationException

fun createGoCodeforcesConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration {
  //  We have InputRedirectOptions only from 203 branch
  return InvalidCodeforcesRunConfiguration(project, factory)
}

fun getGoInputRedirectOptions(configuration: GoCodeforcesRunConfiguration): InputRedirectAware.InputRedirectOptions {
  throw UnsupportedOperationException("Feature is available only from 203 branch")
}
