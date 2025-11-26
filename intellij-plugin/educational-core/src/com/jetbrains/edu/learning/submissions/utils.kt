@file:JvmName("SubmissionUtils")

package com.jetbrains.edu.learning.submissions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColorUtil
import com.intellij.ui.icons.CachedImageIcon
import com.intellij.util.Time
import com.jetbrains.edu.EducationalCoreIcons.Submission.*
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.findTaskFileInDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.TaskToolWindowBundle
import com.jetbrains.edu.learning.ui.EduColors
import java.net.URL
import java.text.DateFormat
import java.util.*

const val OPEN_PLACEHOLDER_TAG = "<placeholder>"
const val CLOSE_PLACEHOLDER_TAG = "</placeholder>"
private const val MAX_FILE_SIZE: Int = 5 * 1024 * 1024 // 5 Mb
private val LOG: Logger = logger<Submission>()

fun getSolutionFiles(project: Project, task: Task): List<SolutionFile> {
  val files = ArrayList<SolutionFile>()
  val taskDir = task.getDir(project.courseDir) ?: error("Failed to find task directory ${task.name}")

  for (taskFile in task.taskFiles.values) {
    val virtualFile = findTaskFileInDirWithSizeCheck(taskFile, taskDir) ?: continue

    ApplicationManager.getApplication().runReadAction {
      val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction
      val text = document.text
      var insertedTextLength = 0
      val builder = StringBuilder(text)
      for (placeholder in taskFile.answerPlaceholders) {
        builder.insert(placeholder.offset + insertedTextLength, OPEN_PLACEHOLDER_TAG)
        builder.insert(placeholder.offset + insertedTextLength + placeholder.length + OPEN_PLACEHOLDER_TAG.length, CLOSE_PLACEHOLDER_TAG)
        insertedTextLength += OPEN_PLACEHOLDER_TAG.length + CLOSE_PLACEHOLDER_TAG.length
      }
      files.add(SolutionFile(taskFile.name, builder.toString(), taskFile.isVisible))
    }
  }

  return files.checkNotEmpty()
}

fun findTaskFileInDirWithSizeCheck(taskFile: TaskFile, taskDir: VirtualFile): VirtualFile? {
  val virtualFile = taskFile.findTaskFileInDir(taskDir) ?: return null
  return if (virtualFile.length > MAX_FILE_SIZE) {
    LOG.warn("File ${virtualFile.path} is too big (${virtualFile.length} bytes), will be ignored for submitting to the server")
    null
  }
  else virtualFile
}

fun List<SolutionFile>.checkNotEmpty(): List<SolutionFile> {
  if (isEmpty()) {
    error("No files were collected to post solution")
  }
  else return this
}

fun isVersionCompatible(submissionFormatVersion: Int): Boolean {
  if (submissionFormatVersion > JSON_FORMAT_VERSION) {
    // TODO: show notification with suggestion to update plugin
    LOG.warn("The plugin supports versions of submission reply not greater than $JSON_FORMAT_VERSION. The current version is `$submissionFormatVersion`")
    return false
  }
  return true
}

internal fun Date.isSignificantlyAfter(otherDate: Date): Boolean {
  val diff = time - otherDate.time
  return diff > Time.SECOND * 10
}

fun formatDate(time: Date): String {
  val calendar = GregorianCalendar()
  calendar.time = time
  val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault())
  return formatter.format(calendar.time).replace("UTC", "(UTC)")
}

fun getImageUrl(status: String?): URL? {
  val icon = when (status) {
    CORRECT -> if (StyleResourcesManager.isHighContrast()) TaskSolvedHighContrast else TaskSolved
    else -> if (StyleResourcesManager.isHighContrast()) TaskFailedHighContrast else TaskFailed
  }

  return (icon as CachedImageIcon).url
}

fun getLinkColor(submission: Submission): String {
  return when (submission.status) {
    CORRECT -> getCorrectLinkColor()
    else -> getWrongLinkColor()
  }
}

private fun getCorrectLinkColor(): String {
  return if (StyleResourcesManager.isHighContrast()) {
    TaskToolWindowBundle.value("correct.label.foreground.high.contrast")
  }
  else {
    "#${ColorUtil.toHex(EduColors.correctLabelForeground)}"
  }
}

private fun getWrongLinkColor(): String {
  return if (StyleResourcesManager.isHighContrast()) {
    TaskToolWindowBundle.value("wrong.label.foreground.high.contrast")
  }
  else {
    "#${ColorUtil.toHex(EduColors.wrongLabelForeground)}"
  }
}