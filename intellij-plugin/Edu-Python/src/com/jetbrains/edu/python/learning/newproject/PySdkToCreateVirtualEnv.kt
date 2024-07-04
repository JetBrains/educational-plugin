package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PyFlavorAndData
import com.jetbrains.python.sdk.flavors.PyFlavorData.Empty

class PySdkToCreateVirtualEnv private constructor(
  name: String,
  path: String,
  version: String
) : ProjectJdkImpl(name, PythonSdkType.getInstance(), path, version) {

  companion object {
    fun create(name: String, path: String, version: String): PySdkToCreateVirtualEnv {
      val sdk = PySdkToCreateVirtualEnv(name, path, version)
      with(sdk.sdkModificator) {
        sdkAdditionalData = PythonSdkAdditionalData(PyFlavorAndData(Empty, FakePythonSdkFlavor))
        // Since this sdk is not associated with anything, it's ok not to use write action here for synchronization.
        // Otherwise, we have to switch to EDT taking into account modality state
        // since this code is supposed to be invoked from BGT from modal dialog
        applyChangesWithoutWriteAction()
      }
      return sdk
    }
  }
}
