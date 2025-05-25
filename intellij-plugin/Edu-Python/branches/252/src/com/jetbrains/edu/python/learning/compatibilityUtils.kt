package com.jetbrains.edu.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.python.packaging.common.PythonSimplePackageSpecification
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.flavors.PyFlavorData
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import com.jetbrains.python.sdk.setAssociationToModule
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