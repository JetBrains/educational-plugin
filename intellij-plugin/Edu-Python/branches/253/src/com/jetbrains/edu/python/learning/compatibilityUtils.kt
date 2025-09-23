package com.jetbrains.edu.python.learning

import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.findPackageSpecification
import com.jetbrains.python.packaging.management.toInstallRequest

// BACKCOMPAT: 2025.2. Inline it.
internal suspend fun installRequiredPackages(
  @Suppress("unused") reporter: SequentialProgressReporter,
  packageManager: PythonPackageManager,
  requirements: List<PyRequirement>
) {
  for (pyRequirement in requirements) {
    reporter.itemStep(pyRequirement.name) {
      val packageSpecification = packageManager.findPackageSpecification(
        packageName = pyRequirement.name,
        versionSpec = pyRequirement.versionSpecs.firstOrNull()
      ) ?: return@itemStep
      packageManager.installPackage(packageSpecification.toInstallRequest())
    }
  }
}
