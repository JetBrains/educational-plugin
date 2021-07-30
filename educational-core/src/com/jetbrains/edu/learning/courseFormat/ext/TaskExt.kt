@file:JvmName("TaskExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilCore
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import kotlin.collections.component1
import kotlin.collections.component2

val Task.project: Project? get() = course.project

val Task.sourceDir: String? get() = course.sourceDir
val Task.testDirs: List<String> get() = course.testDirs

val Task.isFrameworkTask: Boolean get() = lesson is FrameworkLesson

val Task.dirName: String get() = if (isFrameworkTask && course.isStudy) EduNames.TASK else name

fun Task.findDir(lessonDir: VirtualFile?): VirtualFile? {
  return lessonDir?.findChild(dirName)
}

fun Task.findSourceDir(taskDir: VirtualFile): VirtualFile? {
  val sourceDir = sourceDir ?: return null
  return taskDir.findFileByRelativePath(sourceDir)
}

fun Task.findTestDirs(taskDir: VirtualFile): List<VirtualFile> = testDirs.mapNotNull { taskDir.findFileByRelativePath(it) }

fun Task.findTestDirs(project: Project): List<VirtualFile> {
  val taskDir = getDir(project.courseDir) ?: return emptyList()
  return findTestDirs(taskDir)
}

fun Task.getAllTestDirectories(project: Project): List<PsiDirectory> {
  val testDirs = findTestDirs(project)
  return testDirs.mapNotNull { PsiManager.getInstance(project).findDirectory(it) }
}

fun Task.getAllTestFiles(project: Project): List<PsiFile> {
  val testFiles = getAllTestVFiles(project)
  return PsiUtilCore.toPsiFiles(PsiManager.getInstance(project), testFiles)
}

fun Task.getAllTestVFiles(project: Project): MutableList<VirtualFile> {
  val testFiles = mutableListOf<VirtualFile>()
  findTestDirs(project).forEach { testDir ->
    VfsUtilCore.processFilesRecursively(testDir) {
      if (it.isTestsFile(project)) {
        testFiles.add(it)
      }
      true
    }
  }
  return testFiles
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

  val taskDir = getDir(project.courseDir) ?: return
  for ((_, taskFile) in taskFiles) {
    val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
    val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: continue
    for (placeholder in taskFile.answerPlaceholders) {
      val startOffset = placeholder.offset
      val endOffset = placeholder.endOffset
      placeholder.studentAnswer = document.getText(TextRange.create(startOffset, endOffset))
    }
  }
  YamlFormatSynchronizer.saveItem(this)
}

fun Task.addDefaultTaskDescription() {
  val format = EduUtils.getDefaultTaskDescriptionFormat()
  val fileName = format.descriptionFileName
  descriptionText = GeneratorUtils.getInternalTemplateText(fileName)
  descriptionFormat = format
}

fun Task.getDescriptionFile(project: Project): VirtualFile? {
  val taskDir = getDir(project.courseDir) ?: return null
  return taskDir.findChild(descriptionFormat.descriptionFileName)
}

private fun TaskFile.canShowSolution() =
  answerPlaceholders.isNotEmpty() && answerPlaceholders.all { it.possibleAnswer.isNotEmpty() }

fun Task.canShowSolution(): Boolean {
  if (course is HyperskillCourse) {
    return this !is TheoryTask && status == CheckStatus.Solved
  }
  val hiddenByEducator = solutionHidden ?: course.solutionsHidden
  val shouldShow = !hiddenByEducator || status == CheckStatus.Solved
  return shouldShow && taskFiles.values.any { it.canShowSolution() }
}

fun Task.getCodeTaskFile(project: Project): TaskFile? {
  val files = taskFiles.values
  if (files.size == 1) return files.firstOrNull()
  val mainFileName = course.configurator?.courseBuilder?.mainTemplateName
  if (mainFileName != null) {
    val name = GeneratorUtils.joinPaths(sourceDir, mainFileName)
    if (name in taskFiles) {
      return taskFiles[name]
    }
  }
  val editorTaskFile = project.selectedTaskFile
  return if (editorTaskFile?.task == this) {
    editorTaskFile
  }
  else {
    files.firstOrNull { !it.isLearnerCreated && it.isVisible }
  }
}

@JvmName("revertTaskParameters")
fun Task.revertTaskParameters(project: Project) {
  if (this is VideoTask) {
    currentTime = 0
    TaskDescriptionView.getInstance(project).updateTaskDescription()
  }
  else if (this is ChoiceTask) {
    this.clearSelectedVariants()
  }
}

fun Task.shouldBeEmpty(path: String): Boolean {
  return shouldGenerateTestsOnTheFly() &&
         EduUtils.isTestsFile(this, path) &&
         getTaskFile(path)?.isVisible != true
}

fun Task.shouldGenerateTestsOnTheFly(): Boolean {
  val course = lesson?.course ?: return false
  return course.isStudy && course is EduCourse && course.isMarketplace && (this is EduTask || this is OutputTask)
}