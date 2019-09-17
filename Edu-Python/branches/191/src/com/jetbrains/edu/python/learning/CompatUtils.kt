@file:JvmName("CompatUtils")

package com.jetbrains.edu.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.sdk.PythonSdkType

val Sdk.isVirtualEnv: Boolean get() = PythonSdkType.isVirtualEnv(this)

fun Module.findPythonSdk(): Sdk? = PythonSdkType.findPythonSdk(this)
