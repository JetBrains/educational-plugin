@file:JvmName("CompatUtils")

package com.jetbrains.edu.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.sdk.PythonSdkUtil

val Sdk.isVirtualEnv: Boolean get() = PythonSdkUtil.isVirtualEnv(this)

fun Module.findPythonSdk(): Sdk? = PythonSdkUtil.findPythonSdk(this)
