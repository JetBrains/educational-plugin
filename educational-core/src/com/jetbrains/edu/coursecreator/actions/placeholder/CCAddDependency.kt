package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency


class CCAddDependency : CCAnswerPlaceholderAction(null, "Adds/Edits dependency on another answer placeholder") {
  override fun performAnswerPlaceholderAction(state: CCState) {
    val answerPlaceholder = state.answerPlaceholder ?: return
    val validator: InputValidator = object : InputValidatorEx {
      private var errorText = "";
      override fun getErrorText(inputString: String): String? {
        return if (errorText.isEmpty()) null else errorText
      }

      override fun checkInput(inputString: String): Boolean {
        return try {
          val dependency = AnswerPlaceholderDependency.create(answerPlaceholder, inputString)
          if (dependency == null) {
            errorText = "invalid dependency"
            return false
          }
          errorText = ""
          true
        }
        catch (e: AnswerPlaceholderDependency.InvalidDependencyException) {
          errorText = e.customMessage
          false
        }
      }

      override fun canClose(inputString: String): Boolean {
        return errorText.isEmpty()
      }
    }
    val task = answerPlaceholder.taskFile.task
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
    val lesson = task.lesson
    if (task.index == 1 && lesson.index == 1) {
      return
    }
    e.presentation.text = getActionName(answerPlaceholder)
    e.presentation.isEnabledAndVisible = true
  }

  private fun getActionName(answerPlaceholder: AnswerPlaceholder) =
    "${if (answerPlaceholder.placeholderDependency == null) "Add" else "Edit"} Dependency"
}