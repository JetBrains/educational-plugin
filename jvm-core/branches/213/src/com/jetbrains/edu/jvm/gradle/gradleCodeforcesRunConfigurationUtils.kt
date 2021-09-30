@file:JvmName("GradleCodeforcesUtils")

package com.jetbrains.edu.jvm.gradle

import com.intellij.execution.ExecutionException
import com.intellij.execution.application.ApplicationConfiguration.JavaApplicationCommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.TargetEnvironmentRequest
import com.intellij.execution.target.TargetedCommandLineBuilder

@Suppress("UnstableApiUsage")
fun getJavaApplicationCommandLineState(
  configuration: GradleCodeforcesRunConfiguration,
  env: ExecutionEnvironment
): JavaApplicationCommandLineState<GradleCodeforcesRunConfiguration> =
  object : JavaApplicationCommandLineState<GradleCodeforcesRunConfiguration>(configuration, env) {
    @Throws(ExecutionException::class)
    override fun createTargetedCommandLine(request: TargetEnvironmentRequest): TargetedCommandLineBuilder {
      val commandLine = super.createTargetedCommandLine(request)
      val inputFile = configuration.getRedirectInputFile()
      if (inputFile != null) {
        commandLine.setInputFile(request.defaultVolume.createUpload(inputFile.path))
      }
      return commandLine
    }
  }