package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PyFlavorAndData
import com.jetbrains.python.sdk.flavors.PyFlavorData.Empty

// BACKCOMPAT: 2023.3. Make constructor private
class PySdkToCreateVirtualEnv(
  name: String,
  path: String,
  version: String
) : ProjectJdkImpl(name, PythonSdkType.getInstance(), path, version) {

  companion object Companion
}

// BACKCOMPAT: 2023.3. Inline it
fun fakeSdkAdditionalData(): PythonSdkAdditionalData = PythonSdkAdditionalData(PyFlavorAndData(Empty, FakePythonSdkFlavor))
