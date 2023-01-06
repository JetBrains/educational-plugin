package com.jetbrains.edu.android

import com.android.tools.idea.gradle.util.GradleWrapper
import com.intellij.openapi.vfs.VirtualFile

// BACKCOMPAT: 2022.1. Inline it
fun createGradleWrapper(dir: VirtualFile) {
  GradleWrapper.create(dir, GradleWrapper.getGradleVersionToUse(), null)
}
