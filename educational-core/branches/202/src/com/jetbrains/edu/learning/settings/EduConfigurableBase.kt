package com.jetbrains.edu.learning.settings

import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.options.CompositeConfigurable
import com.intellij.openapi.options.Configurable

// BACKCOMPAT: 2019.3. Merge with `EduConfigurable`
abstract class EduConfigurableBase : CompositeConfigurable<OptionsProvider>(), Configurable.WithEpDependencies {
  override fun getDependencies(): Collection<BaseExtensionPointName<*>> = listOf(OptionsProvider.EP_NAME)
}
