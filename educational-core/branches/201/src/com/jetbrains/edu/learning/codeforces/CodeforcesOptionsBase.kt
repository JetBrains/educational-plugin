package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.options.Configurable

// BACKCOMPAT: 2019.3. Merge with `CodeforcesOptions`
abstract class CodeforcesOptionsBase : Configurable, Configurable.WithEpDependencies {
  override fun getDependencies(): Collection<BaseExtensionPointName<*>> {
    return listOf(CodeforcesLanguageProvider.EP_NAME)
  }
}
