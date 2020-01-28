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
