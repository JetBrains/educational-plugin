package com.jetbrains.edu.android

import com.android.tools.idea.gradle.util.GradleWrapper
import com.intellij.openapi.vfs.VirtualFile

fun createGradleWrapper(dir: VirtualFile) {
  GradleWrapper.create(dir, null)
}
