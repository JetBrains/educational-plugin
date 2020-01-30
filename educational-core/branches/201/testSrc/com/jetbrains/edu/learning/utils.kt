package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project

import com.intellij.testFramework.registerComponentInstance

fun createFileEditorManager(project: Project): FileEditorManagerImpl = FileEditorManagerImpl(project)

fun <T : Any> ComponentManager.registerComponent(componentKey: Class<T>, implementation: T, disposable: Disposable): T {
  return registerComponentInstance(componentKey, implementation, disposable)!!
}

// AS relies on a bundle provided by CIDR feature statistic provider, but it is not registered in tests for some reason.
// And it leads to fail of some tests.
// This hack tries to load this provider manually.
//
// Inspired by kotlin plugin
fun registerAdditionalResourceBundleProviders(disposable: Disposable) {
  // TODO: check if we need some implementation of this method for AS based on 2020.1
}
