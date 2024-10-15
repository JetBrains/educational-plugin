package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.util.Key

val ALLOW_IN_LIGHT_PROJECT_KEY: Key<Boolean>
  get() = FileEditorManagerImpl.ALLOW_IN_LIGHT_PROJECT
