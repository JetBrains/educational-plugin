package com.jetbrains.edu.learning.codeforces.run

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.vfs.VirtualFile

interface CodeforcesRunConfiguration: RunConfiguration {
  fun getRedirectInputFile(): VirtualFile?
  fun setExecutableFile(file: VirtualFile)
}
