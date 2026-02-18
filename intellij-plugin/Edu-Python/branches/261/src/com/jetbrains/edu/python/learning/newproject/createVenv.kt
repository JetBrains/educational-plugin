package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.python.venv.createVenv
import com.jetbrains.python.getOrThrow
import com.jetbrains.python.sdk.PyDetectedSdk

// BACKCOMPAT: 2025.3. Inline it.
fun createVenv(baseSdk: PyDetectedSdk, virtualEnvPath: String): String {
  val pythonPath = baseSdk.homePath?.toNioPathOrNull() ?: error("Python home path is not found")
  val virtualEnvNioPath = virtualEnvPath.toNioPathOrNull() ?: error("Virtual env path is not found")
  return runBlockingMaybeCancellable {
    createVenv(pythonPath, virtualEnvNioPath, false)
  }.getOrThrow().toString()
}