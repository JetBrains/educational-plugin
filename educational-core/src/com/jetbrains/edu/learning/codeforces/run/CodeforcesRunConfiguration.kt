package com.jetbrains.edu.learning.codeforces.run

import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

interface CodeforcesRunConfiguration : RunConfiguration {
  @JvmDefault
  fun getRedirectInputFile(): VirtualFile? {
    if (this !is InputRedirectAware) return null
    val path: String = inputRedirectOptions.redirectInputPath ?: return null
    return VfsUtil.findFile(Path.of(path), true)
  }

  fun setExecutableFile(file: VirtualFile)
}
