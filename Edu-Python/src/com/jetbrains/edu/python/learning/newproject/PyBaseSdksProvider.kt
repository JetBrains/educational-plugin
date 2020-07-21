package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

/**
 * The idea of this class is to reuse information about sdk version and python language level
 * if it is already computed
 */
object PyBaseSdksProvider {
  @Volatile
  private var sdkDescriptors: Map<String, PyBaseSdkDescriptor> = emptyMap()

  fun getBaseSdks(context: UserDataHolder? = null): Collection<PyBaseSdkDescriptor> {
    val newDescriptors = mutableMapOf<String, PyBaseSdkDescriptor>()

    val flavor = PythonSdkFlavor.getApplicableFlavors(false).first()
    val sdkPaths = flavor.suggestHomePaths(null, context)
    for (sdkPath in sdkPaths) {
      val sdkDescriptor = sdkDescriptors[sdkPath]
      if (sdkDescriptor != null) {
        newDescriptors[sdkPath] = sdkDescriptor
        continue
      }
      val versionString = flavor.getVersionString(sdkPath) ?: continue
      val languageLevel = flavor.getLanguageLevelFromVersionString(versionString)
      newDescriptors[sdkPath] = PyBaseSdkDescriptor(sdkPath, versionString, languageLevel)
    }

    sdkDescriptors = newDescriptors
    return sdkDescriptors.values
  }
}

class PyBaseSdkDescriptor(val path: String, val version: String, val languageLevel: LanguageLevel)

