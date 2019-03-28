package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CCAddDependency : CCAnswerPlaceholderAction(null, "Adds/Edits dependency on another answer placeholder") {
  override fun performAnswerPlaceholderAction(state: CCState) {
    val answerPlaceholder = state.answerPlaceholder ?: return

    val (dependencyPath, isVisible) = CCDependencyDialog(state.project, answerPlaceholder).showAndGetResult() ?: return

    answerPlaceholder.placeholderDependency = AnswerPlaceholderDependency.create(answerPlaceholder, dependencyPath, isVisible)
    YamlFormatSynchronizer.saveItem(state.taskFile.task)
    EditorNotifications.getInstance(state.project).updateNotifications(state.file.virtualFile)
  }


  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val state = getState(e) ?: return
    val answerPlaceholder = state.answerPlaceholder ?: return
    val task = answerPlaceholder.taskFile.task
    if (task.isFirstInCourse) {
      return
    }
    e.presentation.text = getActionName(answerPlaceholder)
    e.presentation.isEnabledAndVisible = true
  }

  companion object {
    @JvmStatic
    fun getActionName(answerPlaceholder: AnswerPlaceholder) =
      "${if (answerPlaceholder.placeholderDependency == null) "Add" else "Edit"} Dependency"
  }
}

private val Task.isFirstInCourse: Boolean
  get()  {
    if (index > 1) {
      return false
    }
    val section = lesson.section ?: return lesson.index == 1
    return section.index == 1 && lesson.index == 1
  }
