/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.taskDescription.ui

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import javax.swing.JComponent


abstract class TaskDescriptionToolWindow(protected val project: Project) {
  private val HINT_BLOCK_TEMPLATE = "<div class='" + HINT_HEADER + "'>Hint %s</div>" +
                                    "  <div class='hint_content'>" +
                                    " %s" +
                                    "  </div>"
  private val HINT_EXPANDED_BLOCK_TEMPLATE = "<div class='" + HINT_HEADER_EXPANDED + "'>Hint %s</div>" +
                                             "  <div class='hint_content'>" +
                                             " %s" +
                                             "  </div>"

  init {
    // BACKCOMPAT: 2019.2
    @Suppress("DEPRECATION")
    LafManager.getInstance().addLafManagerListener(StudyLafManagerListener(), project)
  }

  abstract fun createTaskInfoPanel(): JComponent

  abstract fun createTaskSpecificPanel(): JComponent

  open fun updateTaskSpecificPanel(task: Task?) {}

  protected fun wrapHints(text: String, task: Task?): String {
    if (task is VideoTask) return text
    val document = Jsoup.parse(text)
    val hints = document.getElementsByClass("hint")
    if (hints.size == 1) {
      val hint = hints[0]
      val hintText = wrapHint(hint, "")
      hint.html(hintText)
      return document.html()
    }
    for (i in hints.indices) {
      val hint = hints[i]
      val hintText = wrapHint(hint, (i + 1).toString())
      hint.html(hintText)
    }
    return document.html()
  }

  protected open fun wrapHint(hintElement: Element, displayedHintNumber: String): String {
    val course = StudyTaskManager.getInstance(project).course
    val hintText: String = hintElement.html()
    if (course == null) {
      return String.format(HINT_BLOCK_TEMPLATE, displayedHintNumber, hintText)
    }

    val study = course.isStudy
    return if (study) {
      String.format(HINT_BLOCK_TEMPLATE, displayedHintNumber, hintText)
    }
    else {
      String.format(HINT_EXPANDED_BLOCK_TEMPLATE, displayedHintNumber, hintText)
    }
  }

  fun setTaskText(project: Project, task: Task?) {
    setText(getTaskDescriptionWithCodeHighlighting(project, task), task)
  }

  abstract fun setText(text: String, task: Task?)

  protected abstract fun updateLaf()

  private inner class StudyLafManagerListener : LafManagerListener {
    override fun lookAndFeelChanged(manager: LafManager) {
      updateLaf()
    }
  }

  companion object {
    const val HINT_HEADER: String = "hint_header"
    const val HINT_HEADER_EXPANDED: String = "${HINT_HEADER} checked"

    @VisibleForTesting
    fun getTaskDescriptionWithCodeHighlighting(project: Project, task: Task?): String {
      if (task != null) {
        val taskText = EduUtils.getTaskTextFromTask(project, task)
        if (taskText != null) {
          if (task is VideoTask) {
            return taskText
          }

          val course = task.course
          val language = if (course is HyperskillCourse) PlainTextLanguage.INSTANCE else course.languageById ?: return taskText
          return EduCodeHighlighter.highlightCodeFragments(project, taskText, language)
        }
      }
      return EduCoreBundle.message("label.open.task")
    }
  }
}
