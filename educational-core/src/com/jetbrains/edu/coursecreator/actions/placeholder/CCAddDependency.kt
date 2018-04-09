package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.tasks.Task


class CCAddDependency : CCAnswerPlaceholderAction(null, "Adds/Edits dependency on another answer placeholder") {
  override fun performAnswerPlaceholderAction(state: CCState) {
    val answerPlaceholder = state.answerPlaceholder ?: return
    val validator: InputValidator = object : InputValidatorEx {
      private var errorText: String? = null
      override fun getErrorText(inputString: String): String? = errorText

      override fun checkInput(inputString: String): Boolean {
        return try {
          val dependency = AnswerPlaceholderDependency.create(answerPlaceholder, inputString)
          if (dependency == null) {
            errorText = "invalid dependency"
            return false
          }
          errorText = null
          true
        }
        catch (e: AnswerPlaceholderDependency.InvalidDependencyException) {
          errorText = e.customMessage
          false
        }
      }

      override fun canClose(inputString: String): Boolean = errorText == null
    }
    val dependency = Messages.showInputDialog(state.project, "", getActionName(answerPlaceholder), null, getInitialValue(answerPlaceholder), validator, null)
    if (dependency.isNullOrEmpty()) {
      return
    }
    answerPlaceholder.placeholderDependency = AnswerPlaceholderDependency.create(answerPlaceholder, dependency!!)
    EditorNotifications.getInstance(state.project).updateNotifications(state.file.virtualFile)
  }

  private fun getInitialValue(answerPlaceholder: AnswerPlaceholder) : String {
    return if (answerPlaceholder.placeholderDependency != null) answerPlaceholder.placeholderDependency.toString() else "lesson1#task1#path/task.txt#1"
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

  private fun getActionName(answerPlaceholder: AnswerPlaceholder) =
    "${if (answerPlaceholder.placeholderDependency == null) "Add" else "Edit"} Dependency"
}

private val Task.isFirstInCourse: Boolean
  get()  {
    if (index > 1) {
      return false
    }
    val section = lesson.section ?: return lesson.index == 1
    return section.index == 1 && lesson.index == 1
  }
