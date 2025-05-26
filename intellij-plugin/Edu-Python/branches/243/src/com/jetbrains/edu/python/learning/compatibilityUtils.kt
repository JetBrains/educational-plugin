package com.jetbrains.edu.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.common.PythonSimplePackageSpecification
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.flavors.PyFlavorData
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import com.jetbrains.python.sdk.setAssociationToModule

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
      packageManager.installPackage(spec, emptyList())
    }
  }
}

internal fun setAssociationToModule(sdk: Sdk, module: Module) {
  sdk.setAssociationToModule(module)
}

internal fun <D : PyFlavorData> getVersionString(flavor: PythonSdkFlavor<D>, sdkPath: String?): String? = flavor.getVersionString(sdkPath)

internal fun <D : PyFlavorData> getLanguageLevelFromVersionStringStatic(flavor: PythonSdkFlavor<D>, versionString: String): LanguageLevel =
  flavor.getLanguageLevelFromVersionString(versionString)