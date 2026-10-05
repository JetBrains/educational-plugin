package com.jetbrains.edu.jvm.environment

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask

fun createIncompleteSdk(sdkModel: ProjectSdksModel, task: SdkDownloadTask): Sdk =
  sdkModel.createIncompleteSdk(JavaSdk.getInstance(), task, null)
