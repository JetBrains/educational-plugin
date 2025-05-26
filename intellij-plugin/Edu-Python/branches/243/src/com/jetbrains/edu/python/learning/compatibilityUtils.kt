package com.jetbrains.edu.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.packaging.common.PythonSimplePackageSpecification
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.sdk.setAssociationToModule

internal suspend fun installRequiredPackage(packageManager: PythonPackageManager, spec: PythonSimplePackageSpecification) {
  packageManager.installPackage(spec, emptyList())
}

internal fun setAssociationToModule(sdk: Sdk, module: Module) {
  sdk.setAssociationToModule(module)
}