package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.intellij.python.pyproject.model.evolution.currentPythonInterpreter as platformCurrentPythonInterpreter

// BACKCOMPAT: 2026.2 inline it
typealias PythonInterpreter = com.intellij.python.sdk.backend.PythonInterpreter

// BACKCOMPAT: 2026.2 inline it
fun Project.currentPythonInterpreter(): PythonInterpreter? = platformCurrentPythonInterpreter()

// BACKCOMPAT: 2026.2 inline it
fun createPythonPackageManager(project: Project, interpreter: PythonInterpreter): PythonPackageManager =
  PythonPackageManager.forPythonInterpreter(project, interpreter)