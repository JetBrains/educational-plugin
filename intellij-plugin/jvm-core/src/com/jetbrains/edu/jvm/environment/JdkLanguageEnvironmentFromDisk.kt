package com.jetbrains.edu.jvm.environment

import com.intellij.openapi.application.EDT
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.util.lang.JavaVersion
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JdkLanguageEnvironmentFromDisk(
  override val model: ProjectSdksModel,
  override val homePath: String,
  override val version: JavaVersion,
  override val itemName: String,
  override val buildSystemSupport: JdkBuildSystemSupport
) : JdkBasedLanguageEnvironment() {

  override suspend fun buildJdk(): Result<Sdk, String> {
    val type = JavaSdk.getInstance()

    val newSdk = model.createSdk(type, homePath)
    if (!type.setupSdkPaths(newSdk, model)) {
      return Err(EduJVMBundle.message("error.jdk.paths.setup.failed"))
    }

    withContext(Dispatchers.EDT) {
      model.addSdk(newSdk)
    }

    return Ok(newSdk)
  }
}