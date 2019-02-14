@file:JvmName("TaskExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.component1
import kotlin.collections.component2

val Task.project: Project? get() = course.project

val Task.sourceDir: String? get() = course.sourceDir
val Task.testDirs: List<String> get() = course.testDirs

val Task.isFrameworkTask: Boolean get() = lesson is FrameworkLesson

val Task.dirName: String get() = if (isFrameworkTask && course.isStudy) EduNames.TASK else name

fun Task.findSourceDir(taskDir: VirtualFile): VirtualFile? {
  val sourceDir = sourceDir ?: return null
  return taskDir.findFileByRelativePath(sourceDir)
}

fun Task.findTestDirs(taskDir: VirtualFile): List<VirtualFile> = testDirs.mapNotNull { taskDir.findFileByRelativePath(it) }

fun Task.findTestDirs(project: Project): List<VirtualFile> {
  val taskDir = getDir(project) ?: return emptyList()
  return findTestDirs(taskDir)
}

val Task.placeholderDependencies: List<AnswerPlaceholderDependency>
  get() = taskFiles.values.flatMap { taskFile -> taskFile.answerPlaceholders.mapNotNull { it.placeholderDependency } }

fun Task.getUnsolvedTaskDependencies(): List<Task> {
  return placeholderDependencies
    .mapNotNull { it.resolve(course)?.taskFile?.task }
    .filter { it.status != CheckStatus.Solved }
    .distinct()
}

fun Task.getDependentTasks(): Set<Task> {
  val course = course
  return course.items.flatMap { item ->
    when (item) {
      is Lesson -> item.getTaskList()
      is Section -> item.lessons.flatMap { it.taskList }
      else -> emptyList()
    }
  }.filterTo(HashSet()) { task ->
    task.placeholderDependencies.any { it.resolve(course)?.taskFile?.task == this }
  }
}

fun Task.hasChangedFiles(project: Project): Boolean {
  for (taskFile in taskFiles.values) {
    val document = taskFile.getDocument(project) ?: continue
    if (document.text != taskFile.text) {
      return true
    }
  }
  return false
}

fun Task.saveStudentAnswersIfNeeded(project: Project) {
  if (lesson !is FrameworkLesson) return

  val taskDir = getTaskDir(project) ?: return
  for ((_, taskFile) in taskFiles) {
    val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
    val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: continue
    for (placeholder in taskFile.answerPlaceholders) {
      val startOffset = placeholder.offset
      val endOffset = startOffset + placeholder.realLength
      placeholder.studentAnswer = document.getText(TextRange.create(startOffset, endOffset))
    }
  }
}

fun Task.addDefaultTaskDescription() {
  val format = EduUtils.getDefaultTaskDescriptionFormat()
  val fileName = format.descriptionFileName
  val template = FileTemplateManager.getDefaultInstance().getInternalTemplate(fileName)
  descriptionText = template.text
  descriptionFormat = format
}

fun Task.getDescriptionFile(project: Project): VirtualFile? {
  val taskDir = getTaskDir(project) ?: return null
  return taskDir.findChild(descriptionFormat.descriptionFileName)
}

fun Task.hasVisibleTaskFilesNotInsideSourceDir(project: Project): Boolean {
  val taskDir = getDir(project) ?: error("Directory for task $name not found")
  val sourceDir = findSourceDir(taskDir) ?: return false
  return taskFiles.values.any {
    if (!it.isVisible) return@any false
    val virtualFile = it.getVirtualFile(project)
    if (virtualFile == null) {
      Logger.getInstance(Task::class.java).error("VirtualFile for ${it.name} not found")
      return@any false
    }

    !VfsUtil.isAncestor(sourceDir, virtualFile, true)
  }
}

private fun TaskFile.canShowSolution() =
  answerPlaceholders.isNotEmpty() && answerPlaceholders.all { it.possibleAnswer.isNotEmpty() }

fun Task.canShowSolution() = taskFiles.values.any { it.canShowSolution() }

fun Task.taskDescriptionHintBlocks(): String {
  val text = StringBuffer()
  val hints = ArrayList<String>()
  for (value in taskFiles.values) {
    for (placeholder in value.answerPlaceholders) {
      if (!placeholder.isVisible) {
        continue
      }
      for (hint in placeholder.hints) {
        if (!hint.isEmpty()) {
          hints.add(hint)
        }
      }
    }
  }

  if (hints.isEmpty()) {
    return ""
  }

  text.append("\n")
  for (hint in hints) {
    text.append("<div class='hint'>")
      .append(hint)
      .append("</div>")
      .append("\n")
  }
  text.append("\n")

  return text.toString()
}