package com.jetbrains.edu.python.learning

import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.packaging.common.PythonPackage
import com.jetbrains.python.packaging.management.PythonPackageManager

suspend fun PythonPackageManager.syncLocked(): PyResult<List<PythonPackage>> = sync()

suspend fun PythonPackageManager.hasRootDependencyFile(): Boolean = getDependencyFile() != null
