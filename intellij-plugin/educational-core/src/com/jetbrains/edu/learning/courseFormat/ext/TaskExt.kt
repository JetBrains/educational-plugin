@file:JvmName("TaskExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.ObjectUtils
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduUtilsKt.convertToHtml
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_HTML
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.removeHyperskillTags
import com.jetbrains.edu.learning.taskToolWindow.replaceActionIDsWithShortcuts
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.yaml.errorHandling.loadingError
import java.io.IOException
import kotlin.collections.component1
import kotlin.collections.component2

val Task.project: Project? get() = course.project

val Task.sourceDir: String? get() = course.sourceDir
val Task.testDirs: List<String> get() = course.testDirs

val Task.isFrameworkTask: Boolean get() = lesson is FrameworkLesson

val Task.dirName: String get() = if (isFrameworkTask && course.isStudy) TASK else name

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
      is Lesson -> item.taskList
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

  val taskDir = getDir(project.courseDir) ?: return
  for ((_, taskFile) in taskFiles) {
    val virtualFile = taskFile.findTaskFileInDir(taskDir) ?: continue
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
  val format = if (CCSettings.getInstance().useHtmlAsDefaultTaskFormat) DescriptionFormat.HTML else DescriptionFormat.MD
  val fileName = format.descriptionFileName
  descriptionText = GeneratorUtils.getInternalTemplateText(fileName)
  descriptionFormat = format
}

/**
 * If [guessFormat] is `false`, the format of the task description file is taken from the [Task.descriptionFormat]
 * field.
 * If [guessFormat] is `true`, [Task.descriptionFormat] field is ignored, and the description file is searched
 * to be either `task.html` or `task.md` with the former having more priority.
 */
fun Task.getDescriptionFile(project: Project, guessFormat: Boolean = false): VirtualFile? {
  val taskDir = getDir(project.courseDir) ?: return null

  val file = if (guessFormat) {
    taskDir.findChild(TASK_HTML) ?: taskDir.findChild(TASK_MD)
  }
  else {
    taskDir.findChild(descriptionFormat.descriptionFileName)
  }

  file ?: LOG.warn("No task description file for $name")

  return file
}

private fun TaskFile.canShowSolution() =
  answerPlaceholders.isNotEmpty() && answerPlaceholders.all { it.possibleAnswer.isNotEmpty() }

fun Task.canShowSolution(): Boolean {
  if (course is HyperskillCourse) {
    return hasSolutions() && status == CheckStatus.Solved
  }
  val hiddenByEducator = solutionHidden ?: course.solutionsHidden
  val shouldShow = !hiddenByEducator || status == CheckStatus.Solved
  return shouldShow && taskFiles.values.any { it.canShowSolution() }
}

fun Task.hasSolutions(): Boolean = course.isMarketplace || this !is TheoryTask && this !is DataTask

fun Task.getCodeTaskFile(project: Project): TaskFile? {

  fun String.getCodeTaskFile(): TaskFile? {
    val name = GeneratorUtils.joinPaths(sourceDir, this)
    return taskFiles[name]
  }

  val files = taskFiles.values
  if (files.size == 1) return files.firstOrNull()
  course.configurator?.courseBuilder?.mainTemplateName(course)?.getCodeTaskFile()?.let { return it }
  course.configurator?.courseBuilder?.taskTemplateName(course)?.getCodeTaskFile()?.let { return it }
  val editorTaskFile = project.selectedTaskFile
  return if (editorTaskFile?.task == this) {
    editorTaskFile
  }
  else {
    files.firstOrNull { !it.isLearnerCreated && it.isVisible }
  }
}

fun Task.revertTaskFiles(project: Project) {
  ApplicationManager.getApplication().runWriteAction {
    for (taskFile in taskFiles.values) {
      taskFile.revert(project)
    }
  }
}

fun Task.revertTaskParameters() {
  status = CheckStatus.Unchecked
  when (this) {
    is ChoiceTask -> {
      clearSelectedVariants()
    }
    is DataTask -> {
      attempt = null
    }
    is SortingBasedTask -> {
      restoreInitialOrdering()
    }
    is TableTask -> {
      clearSelectedVariants()
    }
  }
}

fun Task.shouldBeEmpty(path: String): Boolean {
  return shouldGenerateTestsOnTheFly() &&
         EduUtilsKt.isTestsFile(this, path) &&
         getTaskFile(path)?.isVisible != true
}

fun Task.shouldGenerateTestsOnTheFly(): Boolean {
  val course = lesson.course
  return course.isStudy && course is EduCourse && course.isMarketplace && (this is EduTask || this is OutputTask)
}

fun Task.updateDescriptionTextAndFormat(project: Project) = runReadAction {
  val taskDescriptionFile = getDescriptionFile(project, guessFormat = true)

  if (taskDescriptionFile == null) {
    descriptionFormat = DescriptionFormat.HTML
    descriptionText = EduCoreBundle.message("task.description.not.found")
    return@runReadAction
  }

  try {
    descriptionText = VfsUtil.loadText(taskDescriptionFile)
    descriptionFormat = taskDescriptionFile.toDescriptionFormat()
  }
  catch (e: IOException) {
    LOG.warn("Failed to load text " + taskDescriptionFile.name)
    descriptionFormat = DescriptionFormat.HTML
    descriptionText = EduCoreBundle.message("task.description.not.found")
  }
}

private fun VirtualFile.toDescriptionFormat(): DescriptionFormat {
  return DescriptionFormat.values().firstOrNull { it.fileExtension == extension } ?: loadingError(
    EduCoreBundle.message("yaml.editor.invalid.description"))
}

@RequiresReadLock
fun Task.getTaskTextFromTask(project: Project): String? {
  val lessonDir = lesson.getDir(project.courseDir) ?: return null
  val taskDirectory = if (lesson is FrameworkLesson) lessonDir.findChild(name) else getDir(project.courseDir)
  if (taskDirectory == null) return null
  var text = getTaskTextByTaskName(this, taskDirectory)
  if (text == null) {
    LOG.warn("Cannot find task description file for a task: $name")
    return null
  }
  text = StringUtil.replace(text, "%IDE_NAME%", ApplicationNamesInfo.getInstance().fullProductName)
  val textBuffer = StringBuffer(text)
  replaceActionIDsWithShortcuts(textBuffer)
  if (course is HyperskillCourse) {
    removeHyperskillTags(textBuffer)
  }
  return textBuffer.toString()
}

private fun getTaskTextByTaskName(task: Task, taskDirectory: VirtualFile): String? {
  val taskTextFile = taskDirectory.getTaskTextFile()
  val taskDescription = ObjectUtils.chooseNotNull(taskTextFile?.getTextFromTaskTextFile(), task.descriptionText)

  return if (taskTextFile != null && TASK_MD == taskTextFile.name) {
    convertToHtml(taskDescription)
  }
  else taskDescription
}

private fun VirtualFile.getTaskTextFile(): VirtualFile? {
  var taskTextFile = findChild(TASK_HTML)
  if (taskTextFile == null) {
    taskTextFile = findChild(TASK_MD)
  }
  return taskTextFile
}

private val LOG = logger<Task>()