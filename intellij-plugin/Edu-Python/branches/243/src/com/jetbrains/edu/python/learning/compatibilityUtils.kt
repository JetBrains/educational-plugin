package com.jetbrains.edu.python.learning

import com.jetbrains.python.packaging.common.PythonSimplePackageSpecification
import com.jetbrains.python.packaging.management.PythonPackageManager

internal suspend fun installRequiredPackage(packageManager: PythonPackageManager, spec: PythonSimplePackageSpecification) {
  packageManager.installPackage(spec, emptyList())
}