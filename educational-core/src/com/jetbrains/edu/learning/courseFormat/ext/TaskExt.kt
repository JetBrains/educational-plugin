@file:JvmName("TaskExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val Task.course: Course? get() = lesson?.course

val Task.project: Project? get() = course?.project

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
    .distinct()
}

fun Task.hasChangedFiles(project: Project): Boolean {
  for (taskFile in taskFiles.values) {
    val document = taskFile.getDocument(project) ?: continue
    if (taskFile.text != null && document.text != taskFile.text) {
      return true
    }
  }
  return false
}

fun Task.addDefaultTaskDescription() {
  val fileName = EduUtils.getTaskDescriptionFileName(CCSettings.getInstance().useHtmlAsDefaultTaskFormat())
  val template = FileTemplateManager.getDefaultInstance().getInternalTemplate(fileName) ?: return
  addTaskText(EduNames.TASK, template.text)
}
