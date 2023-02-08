package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

fun suggestSdkPaths(flavor: PythonSdkFlavor, context: UserDataHolder?): Collection<String> {
  return flavor.suggestHomePaths(null, context)
}