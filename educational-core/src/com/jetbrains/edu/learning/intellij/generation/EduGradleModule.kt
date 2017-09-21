package com.jetbrains.edu.learning.intellij.generation

import com.intellij.openapi.vfs.VirtualFile

data class EduGradleModule(val src: VirtualFile, val test: VirtualFile)