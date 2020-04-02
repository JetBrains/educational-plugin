@file:JvmName("JsUtils")

package com.jetbrains.edu.javascript.learning

import com.intellij.lang.javascript.modules.InstallNodeLocalDependenciesAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

fun installNodeDependencies(project: Project, packageJsonFile: VirtualFile) {
  ApplicationManager.getApplication().invokeLater { InstallNodeLocalDependenciesAction.runAndShowConsole(project, packageJsonFile) }
}