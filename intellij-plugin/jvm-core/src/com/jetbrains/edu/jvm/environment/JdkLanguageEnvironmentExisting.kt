package com.jetbrains.edu.jvm.environment

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.util.lang.JavaVersion
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result

class JdkLanguageEnvironmentExisting(
  override val model: ProjectSdksModel,
  override val homePath: String,
  override val version: JavaVersion,
  override val itemName: String,
  override val buildSystemSupport: JdkBuildSystemSupport,

  val existingJdk: Sdk
): JdkBasedLanguageEnvironment() {

  override suspend fun buildJdk(): Result<Sdk, String> = Ok(existingJdk)
}