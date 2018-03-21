package com.jetbrains.edu.learning.placeholderDependencies

import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
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
      runUndoTransparentWriteAction {
        replaceWithListener(project, placeholderToReplace, replacementText)
      }
    }
  }

  private fun replaceWithListener(project: Project, placeholderToReplace: AnswerPlaceholder, replacementText: String) {
    val document = placeholderToReplace.taskFile.getDocument(project)!!
    val startOffset = placeholderToReplace.offset
    val endOffset = startOffset + placeholderToReplace.realLength
    val isFileOpen = FileEditorManager.getInstance(project).isFileOpen(placeholderToReplace.taskFile.getVirtualFile(project)!!)
    val eduDocumentListener = if (isFileOpen) null else EduDocumentListener(placeholderToReplace.taskFile)
    if (eduDocumentListener != null) {
      document.addDocumentListener(eduDocumentListener)
    }
    try {
      document.replaceString(startOffset, endOffset, replacementText)
    }
    finally {
      if (eduDocumentListener != null) {
        document.removeDocumentListener(eduDocumentListener)
      }
    }
  }

  private fun getReplacementText(project: Project, dependency: AnswerPlaceholderDependency): String {
    val course = dependency.answerPlaceholder.taskFile.task.course!!
    val dependencyPlaceholder = dependency.resolve(course)!!
    val dependencyTask = dependencyPlaceholder.taskFile.task
    val dependencyLesson = dependencyTask.lesson
    return if (dependencyLesson is FrameworkLesson && dependencyLesson.currentTaskIndex != dependencyTask.index - 1) {
      dependencyPlaceholder.studentAnswer ?: error("Student answer should be not null here")
    } else {
      val document = dependencyPlaceholder.taskFile.getDocument(project)!!
      val startOffset = dependencyPlaceholder.offset
      val endOffset = startOffset + dependencyPlaceholder.realLength
      document.getText(TextRange.create(startOffset, endOffset))
    }
  }
}