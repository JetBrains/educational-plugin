package com.jetbrains.edu.learning.placeholderDependencies

import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task


object PlaceholderDependencyManager {
  @JvmStatic
  fun currentTaskChanged(project: Project, task: Task) {
    if (CCUtils.isCourseCreator(project)) {
      return
    }

    for (taskFile in task.taskFiles.values) {
      val virtualFile = EduUtils.findTaskFileInDir(taskFile, task.getTaskDir(project) ?: break) ?: continue
      EditorNotifications.getInstance(project).updateNotifications(virtualFile)
    }

    if (task.status != CheckStatus.Unchecked) {
      return
    }
    if (task.placeholderDependencies.isEmpty()) {
      return
    }

    if (task.hasChangedFiles(project)) {
      return
    }

    val unsolvedTasks = task.getUnsolvedTaskDependencies()
    if (unsolvedTasks.isNotEmpty()) {
      //everything will be handled by editor notification
      return
    }

    for (dependency in task.placeholderDependencies) {
      val replacementText = getReplacementText(project, dependency)
      val placeholderToReplace = dependency.answerPlaceholder
      val document = getDocument(project, placeholderToReplace)
      runUndoTransparentWriteAction {
        val startOffset = placeholderToReplace.offset
        val endOffset = startOffset + placeholderToReplace.realLength
        document.replaceString(startOffset, endOffset, replacementText)
      }
    }
  }

  private fun getReplacementText(project: Project, dependency: AnswerPlaceholderDependency): String {
    val course = dependency.answerPlaceholder.taskFile.task.course!!
    val dependencyPlaceholder = dependency.resolve(course)!!
    val document = getDocument(project, dependencyPlaceholder)
    val startOffset = dependencyPlaceholder.offset
    val endOffset = startOffset + dependencyPlaceholder.realLength
    return document.getText(TextRange.create(startOffset, endOffset))
  }

  private fun getDocument(project: Project, placeholder: AnswerPlaceholder): Document {
    val taskFile = placeholder.taskFile
    return taskFile.task.getDocument(project, taskFile)!!
  }
}