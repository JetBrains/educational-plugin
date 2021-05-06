package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.getContainingTask

object CodeforcesUtils {
  fun VirtualFile.isValidCodeforcesTestFolder(task: CodeforcesTask): Boolean {
    return findChild(task.inputFileName) != null && findChild(task.outputFileName) != null
  }

  fun VirtualFile.isTestDataFolder(project: Project, task: CodeforcesTask): Boolean {
    val taskDir = task.getDir(project.courseDir) ?: return false
    return name == CodeforcesNames.TEST_DATA_FOLDER && parent == taskDir
  }

  fun getInputFile(project: Project, selectedFile: VirtualFile): VirtualFile? {
    val task = selectedFile.getContainingTask(project) as? CodeforcesTask ?: return null
    return selectedFile.getTestFolder(project, task)?.findChild(task.inputFileName)
  }

  private fun VirtualFile.getTestFolder(project: Project, task: CodeforcesTask): VirtualFile? {
    var resultCandidate = this
    while (true) {
      if (resultCandidate.name == task.name) break // no need to go up more
      val testDataFolderCandidate = resultCandidate.parent ?: break
      if (testDataFolderCandidate.isTestDataFolder(project, task)) {
        // If it's not valid, we don't want to try another folders for creating configuration
        return if (resultCandidate.isValidCodeforcesTestFolder(task)) resultCandidate else null
      }
      resultCandidate = testDataFolderCandidate
    }

    val allTestFolders = task.getTestFolders(project)
    return if (allTestFolders.size == 1 && isTestDataFolder(project, task)) {
      allTestFolders.firstOrNull { it.isValidCodeforcesTestFolder(task) }
    }
    else {
      null
    }
  }
}
