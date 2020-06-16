package com.jetbrains.edu.learning

import com.intellij.featureStatistics.FeatureStatisticsBundleEP
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project

import com.intellij.testFramework.registerComponentInstance

fun createFileEditorManager(project: Project): FileEditorManagerImpl = FileEditorManagerImpl(project)

fun <T : Any> ComponentManager.registerComponent(componentKey: Class<T>, implementation: T, disposable: Disposable): T {
  return registerComponentInstance(componentKey, implementation, disposable)!!
}

// AS relies on a feature statistic bundle provided by CIDR, but it is not registered in tests for some reason.
// And it leads to fail of some tests.
// This hack register this bundle manually.
//
// Inspired by kotlin plugin
fun registerAdditionalResourceBundleProviders(disposable: Disposable) {
  val extensionPoint = Extensions.getRootArea().getExtensionPoint(FeatureStatisticsBundleEP.EP_NAME)
  if (extensionPoint.extensions.none { it.qualifiedName == TestOCBundleProvider.qualifiedName }) {
    extensionPoint.registerExtension(TestOCBundleProvider, disposable)
  }
}

private object TestOCBundleProvider : FeatureStatisticsBundleEP() {
  init {
    qualifiedName = "messages.OCBundle"
  }
}
