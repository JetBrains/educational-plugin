package com.jetbrains.edu.python.learning

import com.jetbrains.python.packaging.management.PythonPackageManager

suspend fun PythonPackageManager.hasRootDependencyFile(): Boolean = getRootDependenciesFile() != null