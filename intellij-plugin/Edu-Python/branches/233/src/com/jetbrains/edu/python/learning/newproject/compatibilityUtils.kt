package com.jetbrains.edu.python.learning.newproject

fun PySdkToCreateVirtualEnv.Companion.create(name: String, path: String, version: String): PySdkToCreateVirtualEnv {
  return PySdkToCreateVirtualEnv(name, path, version).apply {
    sdkAdditionalData = fakeSdkAdditionalData()
  }
}
