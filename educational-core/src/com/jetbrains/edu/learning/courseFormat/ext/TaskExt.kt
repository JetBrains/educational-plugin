@file:JvmName("TaskExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val Task.course: Course? get() = lesson?.course

val Task.sourceDir: String? get() = course?.sourceDir
val Task.testDir: String? get() = course?.testDir

val Task.testTextMap: Map<String, String> get() {
  val course = course ?: return emptyMap()
  val testDir = course.testDir ?: return emptyMap()
  return if (testDir.isEmpty()) testsText else testsText.mapKeys { (path, _) -> "$testDir/$path" }
}

fun Task.findSourceDir(taskDir: VirtualFile): VirtualFile? {
  val sourceDir = sourceDir ?: return null
  return taskDir.findFileByRelativePath(sourceDir)
}

fun Task.findTestDir(taskDir: VirtualFile): VirtualFile? {
  val testDir = testDir ?: return null
  return taskDir.findFileByRelativePath(testDir)
}

val Task.placeholderDependencies: List<AnswerPlaceholderDependency>
  get() = taskFiles.values.flatMap { it.answerPlaceholders.mapNotNull { it.placeholderDependency } }

fun Task.getUnsolvedTaskDependencies(): List<Task> {
  return placeholderDependencies
    .mapNotNull { it.resolve(course ?: return@mapNotNull null)?.taskFile?.task }
    .filter { it.status != CheckStatus.Solved }
}

fun Task.hasChangedFiles(project: Project): Boolean {
  for (taskFile in taskFiles.values) {
    val document = getDocument(project, taskFile) ?: continue
    if (taskFile.text != null && document.text != taskFile.text) {
      return true
    }
  }
  return false
}

fun Task.getDocument(project: Project, taskFile: TaskFile): Document? {
  val taskDir = getTaskDir(project) ?: return null
  val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: return null
  return FileDocumentManager.getInstance().getDocument(virtualFile)
}
