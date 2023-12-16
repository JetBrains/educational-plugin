package com.jetbrains.edu.learning.codeforces

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

object CodeforcesUtils {
  private fun VirtualFile.isValidCodeforcesTestFolder(task: CodeforcesTask): Boolean {
    return findChild(task.inputFileName) != null && findChild(task.outputFileName) != null
  }

  private fun VirtualFile.isTestDataFolder(project: Project, task: CodeforcesTask): Boolean {
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

  fun CodeforcesTask.getTestFolders(project: Project): Array<out VirtualFile> {
    return getDir(project.courseDir)?.findChild(CodeforcesNames.TEST_DATA_FOLDER)?.children.orEmpty()
      .filter { it.isValidCodeforcesTestFolder(this) }.toTypedArray()
  }

  fun updateCheckStatus(project: Project) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    submissionsManager.course?.allTasks?.forEach { task ->
      val submissions = submissionsManager.getSubmissions(task)
      val currentStatus = task.status
      if (submissions.isNotEmpty()) {
        task.status = if (submissions.any { it.taskId == task.id && it.status == CORRECT }) {
          CheckStatus.Solved
        }
        else {
          CheckStatus.Failed
        }
        if (currentStatus != task.status) {
          YamlFormatSynchronizer.saveItem(task)
          ProjectView.getInstance(project).refresh()
        }
      }
    }
  }
}
