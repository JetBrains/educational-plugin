package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PySdkToInstall
import com.jetbrains.python.sdk.detectSystemWideSdks
import com.jetbrains.python.sdk.setAssociationToModule

// BACKCOMPAT: 2024.1. Inline it
fun Sdk.setAssociationToModule(project: Project) {
  val module = ModuleManager.getInstance(project).sortedModules.firstOrNull() ?: return
  setAssociationToModule(module)
}

private val LOG = Logger.getInstance("com.jetbrains.edu.python.learning.newproject")

// BACKCOMPAT: 2024.1. Inline it
@RequiresEdt
fun PySdkToInstall.install(): PyDetectedSdk? = install(null) { detectSystemWideSdks(null, emptyList()) }.getOrElse {
  LOG.warn(it)
  null
}
