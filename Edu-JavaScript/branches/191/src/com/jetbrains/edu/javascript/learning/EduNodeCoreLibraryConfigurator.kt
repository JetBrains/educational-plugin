@file:JvmName("EduNodeJsCoreLibraryConfigurator")

package com.jetbrains.edu.javascript.learning

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.library.core.NodeCoreLibraryConfigurator
import com.intellij.openapi.project.Project
import com.intellij.util.text.SemVer

fun configureAndAssociateWithProject(project: Project, interpreter: NodeJsInterpreter, version: SemVer) {
  val configurator = NodeCoreLibraryConfigurator.getInstance(project)
  configurator.configureAndAssociateWithProject(interpreter, version, null)
}