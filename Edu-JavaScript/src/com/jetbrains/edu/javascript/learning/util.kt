@file:JvmName("JsUtils")

package com.jetbrains.edu.javascript.learning

import com.intellij.javascript.nodejs.npm.InstallNodeLocalDependenciesAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

const val NodeJS = "Node.js"

fun installNodeDependencies(project: Project, packageJsonFile: VirtualFile) {
  ApplicationManager.getApplication().invokeLater { InstallNodeLocalDependenciesAction.runAndShowConsole(project, packageJsonFile) }
}