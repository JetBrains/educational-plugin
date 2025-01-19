package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.openapi.Disposable
import com.intellij.util.ui.Animator

// BACKCOMPAT: 2024.2. Inline it.
fun Animator.toDisposable(): Disposable = Disposable {
  this.dispose()
}