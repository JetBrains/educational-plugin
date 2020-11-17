package com.jetbrains.edu.learning.codeforces


import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask

object CodeforcesUtils {
  fun VirtualFile.isValidCodeforcesTestFolder(task: CodeforcesTask): Boolean {
    return findChild(task.inputFileName) != null && findChild(task.outputFileName) != null
  }
}
