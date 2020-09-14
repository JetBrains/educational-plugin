package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils.DEFAULT_PLACEHOLDER_TEXT
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator

abstract class CCAddAnswerPlaceholderActionTestBase : CCAnswerPlaceholderTestBase() {

  class CCTestAddAnswerPlaceholder(val dependencyInfo: CCCreateAnswerPlaceholderDialog.DependencyInfo? = null) : CCAddAnswerPlaceholder() {
    override fun createDialog(project: Project, answerPlaceholder: AnswerPlaceholder): CCCreateAnswerPlaceholderDialog {
      return object : CCCreateAnswerPlaceholderDialog(project, false, answerPlaceholder) {
        override fun showAndGet(): Boolean = true
        override fun getPlaceholderText(): String =
          StudyTaskManager.getInstance(project).course?.configurator?.defaultPlaceholderText ?: DEFAULT_PLACEHOLDER_TEXT
        override fun getDependencyInfo(): DependencyInfo? = dependencyInfo
      }
    }
  }

  data class Selection(val start: Int, val end: Int) {
    val length = end - start
  }

  protected fun TaskFile.createExpectedPlaceholder(offset: Int, text: String, possibleAnswer: String, index: Int = 0): AnswerPlaceholder {

    val placeholderExpected = AnswerPlaceholder()
    placeholderExpected.offset = offset
    placeholderExpected.length = possibleAnswer.length
    placeholderExpected.index = index
    placeholderExpected.taskFile = this
    placeholderExpected.placeholderText = text
    answerPlaceholders.add(placeholderExpected)
    return placeholderExpected
  }
}