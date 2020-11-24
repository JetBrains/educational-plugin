package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseDir

object CodeforcesUtils {
  fun VirtualFile.isValidCodeforcesTestFolder(task: CodeforcesTask): Boolean {
    return findChild(task.inputFileName) != null && findChild(task.outputFileName) != null
  }

  fun VirtualFile.isTestDataFolder(project: Project, task: CodeforcesTask): Boolean {
    val taskDir = task.getDir(project.courseDir) ?: return false
    return name == CodeforcesNames.TEST_DATA_FOLDER && parent == taskDir
  }
}
