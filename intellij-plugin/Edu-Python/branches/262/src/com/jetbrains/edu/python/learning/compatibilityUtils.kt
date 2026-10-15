package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.python.packaging.management.PythonPackageManager

// BACKCOMPAT: 2026.2
typealias PythonInterpreter = Sdk

fun Project.currentPythonInterpreter(): PythonInterpreter? = ProjectRootManager.getInstance(this).projectSdk

fun createPythonPackageManager(project: Project, interpreter: PythonInterpreter): PythonPackageManager =
  PythonPackageManager.forSdk(project, interpreter)