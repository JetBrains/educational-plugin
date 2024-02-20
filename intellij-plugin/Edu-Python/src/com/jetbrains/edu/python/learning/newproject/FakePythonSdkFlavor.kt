package com.jetbrains.edu.python.learning.newproject

import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.sdk.flavors.PyFlavorData
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

/**
 * Helps to avoid unexpected fake python sdk validation
 */
object FakePythonSdkFlavor : PythonSdkFlavor<PyFlavorData.Empty>() {
  override fun getName(): String = "Python"
  override fun sdkSeemsValid(sdk: Sdk, flavorData: PyFlavorData.Empty, targetConfig: TargetEnvironmentConfiguration?): Boolean = true
  override fun getFlavorDataClass(): Class<PyFlavorData.Empty> = PyFlavorData.Empty::class.java
}
