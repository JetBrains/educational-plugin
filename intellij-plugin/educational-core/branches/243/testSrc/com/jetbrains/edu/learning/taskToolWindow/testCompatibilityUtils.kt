package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

// BACKCOMPAT: 2024.2 inline it
fun TaskToolWindowStateTest.allowFileEditorInLightProject(project: Project) {
  project.putUserData(FileEditorManagerKeys.ALLOW_IN_LIGHT_PROJECT, true)
  Disposer.register(testRootDisposable) { project.putUserData(FileEditorManagerKeys.ALLOW_IN_LIGHT_PROJECT, null) }
}
