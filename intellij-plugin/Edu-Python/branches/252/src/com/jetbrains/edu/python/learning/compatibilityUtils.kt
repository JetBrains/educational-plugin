package com.jetbrains.edu.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.toInstallRequest
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.flavors.PyFlavorData
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import com.jetbrains.python.sdk.setAssociationToModule

// BACKCOMPAT: 2025.1. Inline it.
internal suspend fun installRequiredPackages(
  @Suppress("unused") reporter: SequentialProgressReporter,
  packageManager: PythonPackageManager,
  requirements: List<PyRequirement>
) {
  for (pyRequirement in requirements) {
    reporter.itemStep(pyRequirement.name) {
      val packageSpecification = packageManager.findPackageSpecificationWithVersionSpec(
        packageName = pyRequirement.name,
        versionSpec = pyRequirement.versionSpecs.firstOrNull()
      ) ?: return@itemStep
      packageManager.installPackage(packageSpecification.toInstallRequest())
    }
  }
}

// BACKCOMPAT: 2025.1. Inline it.
internal fun setAssociationToModule(sdk: Sdk, module: Module) {
  runWithModalProgressBlocking(module.project, "") {
    sdk.setAssociationToModule(module)
  }
}

// BACKCOMPAT: 2024.3. Inline it.
internal fun <D : PyFlavorData> getVersionString(
  @Suppress("unused") flavor: PythonSdkFlavor<D>,
  sdkPath: String?
): String? = PythonSdkFlavor.getVersionStringStatic(sdkPath)

// BACKCOMPAT: 2024.3. Inline it.
internal fun <D : PyFlavorData> getLanguageLevelFromVersionStringStatic(
  @Suppress("unused") flavor: PythonSdkFlavor<D>,
  versionString: String
): LanguageLevel = PythonSdkFlavor.getLanguageLevelFromVersionStringStatic(versionString)