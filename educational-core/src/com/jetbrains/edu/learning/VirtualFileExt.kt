@file:JvmName("VirtualFileExt")

package com.jetbrains.edu.learning

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduDocumentListener.Companion.runWithListener
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.apache.commons.codec.binary.Base64
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

fun VirtualFile.getEditor(project: Project): Editor? {
  val selectedEditor = getInEdt { FileEditorManager.getInstance(project).getSelectedEditor(this) }
  return if (selectedEditor is TextEditor) selectedEditor.editor else null
}

val VirtualFile.document
  get() : Document = FileDocumentManager.getInstance().getDocument(this) ?: error("Cannot find document for a file: ${name}")

fun VirtualFile.startLoading(project: Project) {
  val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(this) ?: return

  fileEditor.setViewer(true)

  fileEditor.loadingPanel?.apply {
    setLoadingText(EduCoreBundle.message("editor.loading.solution"))
    startLoading()
  }
}

fun VirtualFile.stopLoading(project: Project) {
  val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(this) ?: return

  fileEditor.setViewer(false)
  fileEditor.loadingPanel?.stopLoading()
}

private fun FileEditor.setViewer(isViewer: Boolean) {
  val textEditor = this as? TextEditor ?: return
  (textEditor.editor as EditorEx).isViewer = isViewer
}

private val FileEditor.loadingPanel: JBLoadingPanel?
  get() = UIUtil.findComponentOfType(component, JBLoadingPanel::class.java)

fun VirtualFile.getSection(project: Project): Section? {
  val course = project.course ?: return null
  if (!isDirectory) return null
  return if (project.courseDir == parent) course.getSection(name) else null
}

fun VirtualFile.isSectionDirectory(project: Project): Boolean {
  return getSection(project) != null
}

fun VirtualFile.getLesson(project: Project): Lesson? {
  val course = project.course ?: return null
  if (!isDirectory) return null
  if (parent == null) return null

  val section = parent.getSection(project)
  if (section != null) {
    return section.getLesson(name)
  }
  return if (project.courseDir == parent) course.getLesson(name) else null
}

fun VirtualFile.isLessonDirectory(project: Project): Boolean {
  return getLesson(project) != null
}

fun VirtualFile.getContainingTask(project: Project): Task? {
  val course = project.course ?: return null
  val taskDir = getTaskDir(project) ?: return null
  val lessonDir = taskDir.parent ?: return null
  val lesson = lessonDir.getLesson(project) ?: return null
  return if (lesson is FrameworkLesson && course.isStudy) {
    lesson.currentTask()
  }
  else {
    lesson.getTask(taskDir.name)
  }
}

fun VirtualFile.getTask(project: Project): Task? {
  if (!isDirectory) return null
  val lesson: Lesson = parent?.getLesson(project) ?: return null
  return lesson.getTask(name)
}

fun VirtualFile.isTaskDirectory(project: Project): Boolean {
  return getTask(project) != null
}

fun VirtualFile.getStudyItem(project: Project): StudyItem? {
  val course = project.course ?: return null
  val courseDir = project.courseDir
  if (courseDir == this) return course
  val section = getSection(project)
  if (section != null) return section
  val lesson = getLesson(project)
  return lesson ?: getTask(project)
}

/**
 * @return true, if file doesn't belong to task (in term of course structure)
 * but can be added to it as task, test or additional file.
 * Otherwise, returns false
 */
fun VirtualFile.canBeAddedToTask(project: Project): Boolean {
  if (isDirectory) return false
  val configurator = getContainingTask(project)?.course?.configurator ?: return false
  return if (configurator.excludeFromArchive(project, this)) false else !belongToTask(project)
}

/**
 * @return true, if some task contains given `file` as task, test or additional file.
 * Otherwise, returns false
 */
fun VirtualFile.belongToTask(project: Project): Boolean {
  val task = getContainingTask(project) ?: return false
  val relativePath = pathRelativeToTask(project)
  return task.getTaskFile(relativePath) != null
}

fun VirtualFile.canBelongToCourse(project: Project): Boolean {
  if (isSectionDirectory(project) || isLessonDirectory(project) || isTaskDirectory(project)) return true
  return if (isDirectory) {
    getContainingTask(project) != null
  }
  else {
    belongToTask(project)
  }
}

fun VirtualFile.pathRelativeToTask(project: Project): String {
  val taskDir = getTaskDir(project) ?: return name
  return FileUtil.getRelativePath(taskDir.path, path, VfsUtilCore.VFS_SEPARATOR_CHAR) ?: return name
}

fun VirtualFile.getTaskDir(project: Project): VirtualFile? {
  var taskDir = this
  while (true) {
    val lessonDirCandidate = taskDir.parent ?: return null
    val lesson = lessonDirCandidate.getLesson(project)
    if (lesson != null) {
      if (lesson is FrameworkLesson && EduNames.TASK == taskDir.name || lesson.getTask(taskDir.name) != null) {
        return taskDir
      }
    }
    taskDir = lessonDirCandidate
  }
}

fun VirtualFile.isTestsFile(project: Project): Boolean {
  if (isDirectory) return false
  val task = getContainingTask(project) ?: return false
  val path: String = pathRelativeToTask(project)
  val course = StudyTaskManager.getInstance(project).course ?: return false
  val configurator = course.configurator ?: return false
  return configurator.isTestFile(task, path)
}

fun VirtualFile.getTaskFile(project: Project): TaskFile? {
  val task = getContainingTask(project)
  return task?.getTaskFile(pathRelativeToTask(project))
}

fun VirtualFile.isToEncodeContent(): Boolean {
  val extension = FileUtilRt.getExtension(name)
  val fileType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension(extension)
  if (fileType !is UnknownFileType) {
    return fileType.isBinary
  }
  val contentType = mimeType() ?: return isGitObject(name)
  return contentType.startsWith("image") ||
         contentType.startsWith("audio") ||
         contentType.startsWith("video") ||
         contentType.startsWith("application")
}

fun VirtualFile.mimeType(): String? {
  return try {
    Files.probeContentType(Paths.get(path))
  }
  catch (e: IOException) {
    LOG.error(e)
    null
  }
}

fun VirtualFile.toStudentFile(project: Project, task: Task): TaskFile? {
  try {
    val taskCopy = task.copy()
    val taskFile = taskCopy.getTaskFile(pathRelativeToTask(project)) ?: return null
    if (isToEncodeContent()) {
      taskFile.setText(Base64.encodeBase64String(contentsToByteArray()))
      return taskFile
    }
    FileDocumentManager.getInstance().saveDocument(document)
    val studentFile = LightVirtualFile("student_task", PlainTextFileType.INSTANCE, document.text)
    runWithListener(project, taskFile, studentFile) { studentDocument: Document ->
      for (placeholder in taskFile.answerPlaceholders) {
        try {
          placeholder.possibleAnswer = studentDocument.getText(TextRange.create(placeholder.offset, placeholder.endOffset))
          EduUtils.replaceAnswerPlaceholder(studentDocument, placeholder)
        }
        catch (e: IndexOutOfBoundsException) {
          // We are here because placeholder is broken. We need to put broken placeholder into exception.
          // We need to take it from original task, because taskCopy has issues with links (taskCopy.lesson is always null)
          val file = task.getTaskFile(taskFile.name)
          val answerPlaceholder = file?.answerPlaceholders?.get(placeholder.index)
          throw BrokenPlaceholderException(EduCoreBundle.message("exception.broken.placeholder.title"), answerPlaceholder ?: placeholder)
        }
      }
      taskFile.setText(studentDocument.immutableCharSequence.toString())
    }
    return taskFile
  }
  catch (e: IOException) {
    LOG.error("Failed to convert `${path}` to student file")
  }
  return null
}

private fun isGitObject(name: String): Boolean {
  return (name.length == 38 || name.length == 40) && name.matches(Regex("[a-z0-9]+"))
}

private val LOG = Logger.getInstance("com.jetbrains.edu.learning.VirtualFileExt")
