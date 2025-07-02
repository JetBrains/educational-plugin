package com.jetbrains.edu.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.common.PythonSimplePackageSpecification
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.sdk.setAssociationToModule
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions

internal suspend fun installRequiredPackages(
  reporter: SequentialProgressReporter,
  packageManager: PythonPackageManager,
  requirements: List<PyRequirement>
) {
  for (requirement in requirements) {
    val spec = PythonSimplePackageSpecification(
      requirement.installOptions.joinToString(" "),
      version = null,
      repository = null,
      relation = null
    )

    reporter.itemStep(requirement.name) {
      try {
        packageManager.installPackage(spec, emptyList(), withBackgroundProgress = true)
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
  }
}

internal fun setAssociationToModule(sdk: Sdk, module: Module) {
  sdk.setAssociationToModule(module)
}
