package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.util.Key

// BACKCOMPAT: 2024.2 inline it
val ALLOW_IN_LIGHT_PROJECT_KEY: Key<Boolean>
  get() = FileEditorManagerKeys.ALLOW_IN_LIGHT_PROJECT

