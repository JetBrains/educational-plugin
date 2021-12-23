package com.jetbrains.edu.learning.codeforces.run

import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.text.nullize
import java.nio.file.Path

interface CodeforcesRunConfiguration : RunConfiguration, InputRedirectAware {
  fun getRedirectInputFile(): VirtualFile? {
    val path: String = inputRedirectOptions.redirectInputPath.nullize() ?: return null
    return VfsUtil.findFile(Path.of(path), true)
  }

  fun setExecutableFile(file: VirtualFile) {}
}
