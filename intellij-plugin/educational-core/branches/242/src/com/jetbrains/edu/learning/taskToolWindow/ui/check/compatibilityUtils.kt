package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.openapi.Disposable
import com.intellij.util.ui.Animator

fun Animator.toDisposable(): Disposable = this