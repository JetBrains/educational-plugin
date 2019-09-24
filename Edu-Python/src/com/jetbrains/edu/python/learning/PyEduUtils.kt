package com.jetbrains.edu.python.learning

import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun Task.getCurrentTaskVirtualFile(project: Project): VirtualFile? {
  val taskDir = getTaskDir(project) ?: error("Failed to get task dir for `${name}` task")
  var resultFile: VirtualFile? = null
  for ((_, taskFile) in taskFiles) {
    val file = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
    if (EduUtils.isTestsFile(project, file) || !TextEditorProvider.isTextFile(file)) continue
    if (resultFile == null) {
      resultFile = file
    }

    // TODO: Come up with a smarter way how to find correct task file
    // Try to find task file with new placeholder. See https://youtrack.jetbrains.com/issue/EDU-1443
    val hasNewPlaceholder = taskFile.answerPlaceholders.any { p -> p.placeholderDependency == null }
    if (hasNewPlaceholder) return file
  }
  return resultFile
}

fun Task.getCurrentTaskFilePath(project: Project): String? {
  return getCurrentTaskVirtualFile(project)?.systemDependentPath
}

private val VirtualFile.systemDependentPath: String get() = FileUtil.toSystemDependentName(path)
