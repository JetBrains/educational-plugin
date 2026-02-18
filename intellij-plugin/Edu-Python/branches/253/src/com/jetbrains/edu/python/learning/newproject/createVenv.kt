package com.jetbrains.edu.python.learning.newproject

import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.sdk.PyDetectedSdk

fun createVenv(baseSdk: PyDetectedSdk, virtualEnvPath: String): String {
  val packageManager = PyPackageManager.getInstance(baseSdk)
  return packageManager.createVirtualEnv(virtualEnvPath, false)
}