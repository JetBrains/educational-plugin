package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.testFramework.ThreadTracker


fun longRunningThreadCreated(parentDisposable: Disposable, vararg threadNamePrefixes: String) {
  ThreadTracker.longRunningThreadCreated(parentDisposable, *threadNamePrefixes)
}