package com.jetbrains.edu.python.learning

import com.jetbrains.python.packaging.common.PythonSimplePackageSpecification
import com.jetbrains.python.packaging.management.PythonPackageManager
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions

// BACKCOMPAT: 2024.3. inline it
internal suspend fun installRequiredPackage(packageManager: PythonPackageManager, spec: PythonSimplePackageSpecification) {
  try {
    packageManager.installPackage(spec, emptyList(), withBackgroundProgress = false)
  }
  catch (_: NoSuchMethodError) {
    // For versions of the py plugin before 251.25410. The installPackage() method didn't have the last boolean argument

    val method = packageManager::class.functions.find { function ->
      // Match name and parameter types
      function.name == "installPackage" &&
      function.parameters.size == 3 // 1 receiver + 2 arguments
    } ?: throw NoSuchMethodError("installPackage() method not found")

    method.callSuspend(packageManager, spec, emptyList<String>())
  }
}