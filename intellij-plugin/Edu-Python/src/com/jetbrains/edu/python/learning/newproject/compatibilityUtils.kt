package com.jetbrains.edu.python.learning.newproject

// BACKCOMPAT: 2023.3. Move the method inside `PySdkToCreateVirtualEnv.Companion`
fun PySdkToCreateVirtualEnv.Companion.create(name: String, path: String, version: String): PySdkToCreateVirtualEnv {
  val sdk = PySdkToCreateVirtualEnv(name, path, version)
  with(sdk.sdkModificator) {
    sdkAdditionalData = fakeSdkAdditionalData()
    // Since this sdk is not associated with anything, it's ok not to use write action here for synchronization.
    // Otherwise, we have to switch to EDT taking into account modality state
    // since this code is supposed to be invoked from BGT from modal dialog
    applyChangesWithoutWriteAction()
  }
  return sdk
}
