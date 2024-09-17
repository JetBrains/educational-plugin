package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.sdk.setAssociationToModule

// BACKCOMPAT: 2024.1. Inline it
fun Sdk.setAssociationToModule(project: Project) {
  val module = ModuleManager.getInstance(project).sortedModules.firstOrNull() ?: return
  setAssociationToModule(module)
}
