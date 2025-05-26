package com.jetbrains.edu.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.PythonPackagesInstaller
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.flavors.PyFlavorData
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import com.jetbrains.python.sdk.setAssociationToModule
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

// BACKCOMPAT: 2025.1. Inline it.
internal suspend fun installRequiredPackages(
  @Suppress("unused") reporter: SequentialProgressReporter,
  packageManager: PythonPackageManager,
  requirements: List<PyRequirement>
) {
  val method = PythonPackagesInstaller::class.companionObject?.functions?.find { function ->
    function.name == "installWithRequirements"
    && function.parameters.size == 4 // 1 receiver, 3 arguments
  } ?: throw NoSuchMethodError("No static `installWithRequirements` method was found")
  method.isAccessible = true
  method.callSuspend(PythonPackagesInstaller.Companion, packageManager, requirements, emptyList<String>())
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