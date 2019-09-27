package com.jetbrains.edu.learning

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
