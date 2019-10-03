@file:JvmName("CompatUtils")

package com.jetbrains.edu.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

val Sdk.isVirtualEnv: Boolean get() = PythonSdkType.isVirtualEnv(this)

fun Module.findPythonSdk(): Sdk? = PythonSdkType.findPythonSdk(this)

fun PythonSdkFlavor.homePaths(module: Module?, context: UserDataHolder?): List<String> = suggestHomePaths(module).toList()
