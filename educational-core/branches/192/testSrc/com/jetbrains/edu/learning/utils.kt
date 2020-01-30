@file:Suppress("UnstableApiUsage", "DEPRECATION")

package com.jetbrains.edu.learning

import com.intellij.featureStatistics.FeatureStatisticsBundleProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.impl.ComponentManagerImpl
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

fun createFileEditorManager(project: Project): FileEditorManagerImpl = FileEditorManagerImpl(project)

fun <T> ComponentManager.registerComponent(componentKey: Class<T>, implementation: T, disposable: Disposable) {
  val oldValue = (this as ComponentManagerImpl).registerComponentInstance(componentKey, implementation)
  Disposer.register(disposable, Disposable {
    if (!isDisposed) {
      registerComponentInstance(componentKey, oldValue)
    }
  })
}

private const val CIDR_FEATURE_STATISTICS_PROVIDER_FQNAME = "com.jetbrains.cidr.lang.OCFeatureStatisticsBundleProvider"

// AS relies on a bundle provided by CIDR feature statistic provider, but it is not registered in tests for some reason.
// And it leads to fail of some tests.
// This hack tries to load this provider manually.
//
// Inspired by kotlin plugin
fun registerAdditionalResourceBundleProviders(disposable: Disposable) {
  val isAlreadyRegistered = FeatureStatisticsBundleProvider.EP_NAME.extensions.any { provider ->
    provider.javaClass.name == CIDR_FEATURE_STATISTICS_PROVIDER_FQNAME
  }
  if (isAlreadyRegistered) return

  val providerClass = try {
    Class.forName(CIDR_FEATURE_STATISTICS_PROVIDER_FQNAME)
  } catch (_: ClassNotFoundException) {
    return
  }

  val provider = providerClass.newInstance() as FeatureStatisticsBundleProvider
  FeatureStatisticsBundleProvider.EP_NAME.getPoint(null).registerExtension(provider, disposable)
}
