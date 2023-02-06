package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.testFramework.common.ThreadLeakTracker


// BACKCOMPAT: 2022.3 Inline
fun longRunningThreadCreated(parentDisposable: Disposable, vararg threadNamePrefixes: String) {
  ThreadLeakTracker.longRunningThreadCreated(parentDisposable, *threadNamePrefixes)
}