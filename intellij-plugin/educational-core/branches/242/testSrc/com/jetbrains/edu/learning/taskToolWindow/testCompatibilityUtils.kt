package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

fun TaskToolWindowStateTest.allowFileEditorInLightProject(project: Project) {
  project.putUserData(FileEditorManagerImpl.ALLOW_IN_LIGHT_PROJECT, true)
  Disposer.register(testRootDisposable) { project.putUserData(FileEditorManagerImpl.ALLOW_IN_LIGHT_PROJECT, null) }
}
