package com.jetbrains.edu.python.learning.newproject

import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

fun createDefaultSettings(sdkPath: String): Result<PyLanguageEnvironment, String> {
  val versionString = PythonSdkFlavor.getApplicableFlavors(false).firstOrNull()?.getVersionString(sdkPath)
                      ?: return Err("Can't get python version")
  val sdk = PySdkToCreateVirtualEnv.create(versionString, sdkPath, versionString)
  return Ok(PyLanguageEnvironment(sdk, sdk.languageLevel))
}