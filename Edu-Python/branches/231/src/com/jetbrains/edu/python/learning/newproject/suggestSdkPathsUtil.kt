package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import kotlin.io.path.pathString

fun suggestSdkPaths(flavor: PythonSdkFlavor<*>, context: UserDataHolder?): Collection<String> {
  return flavor.suggestLocalHomePaths(null, context).map { it.pathString }
}