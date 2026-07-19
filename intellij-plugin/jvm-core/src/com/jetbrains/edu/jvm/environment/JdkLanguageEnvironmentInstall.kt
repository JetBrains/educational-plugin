package com.jetbrains.edu.jvm.environment

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloadUtil
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.util.lang.JavaVersion
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result

class JdkLanguageEnvironmentInstall(
  override val model: ProjectSdksModel,
  override val homePath: String,
  override val version: JavaVersion,
  override val itemName: String,
  override val buildSystemSupport: JdkBuildSystemSupport,

  val downloadSize: String,
  val incompleteJdk: Sdk
): JdkBasedLanguageEnvironment() {

  override suspend fun buildJdk(): Result<Sdk, String> {
    if (!JdkDownloadUtil.downloadSdk(incompleteJdk)) return Err(EduJVMBundle.message("error.jdk.download.failed"))
    val downloadedJdk = model.findSdk(incompleteJdk) ?: return Err(EduJVMBundle.message("error.jdk.downloaded.failed.to.set.up"))
    return Ok(downloadedJdk)
  }
}