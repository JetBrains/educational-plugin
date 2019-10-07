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
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import javax.swing.JComponent


abstract class TaskDescriptionToolWindow {
  init {
    LafManager.getInstance().addLafManagerListener(StudyLafManagerListener())
  }

  abstract fun createTaskInfoPanel(project: Project): JComponent

  abstract fun createTaskSpecificPanel(currentTask: Task?): JComponent

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

  protected abstract fun wrapHint(hintText: Element, displayedHintNumber: String): String

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
    private const val EMPTY_TASK_TEXT = "Please, open any task to see task description"
    const val PSI_ELEMENT_PROTOCOL = DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL

    @VisibleForTesting
    fun getTaskDescriptionWithCodeHighlighting(project: Project, task: Task?): String {
      if (task != null) {
        val taskText = EduUtils.getTaskTextFromTask(project, task)
        if (taskText != null) {
          if (task is VideoTask) {
            return taskText
          }
          return EduCodeHighlighter.highlightCodeFragments(project, taskText, task.course.languageById!!)
        }
      }
      return EMPTY_TASK_TEXT
    }

    @JvmStatic
    fun navigateToPsiElement(project: Project, url: String) {
      val qualifiedName = url.replace(PSI_ELEMENT_PROTOCOL, "")

      val application = ApplicationManager.getApplication()
      application.invokeLater {
        application.runReadAction {
          for (provider in QualifiedNameProvider.EP_NAME.extensionList) {
            val element = provider.qualifiedNameToElement(qualifiedName, project)
            if (element is NavigatablePsiElement) {
              val navigatableElement = element as NavigatablePsiElement?
              if (navigatableElement!!.canNavigate()) {
                navigatableElement.navigate(true)
              }
              break
            }
          }
        }
      }
      EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.PSI)
    }
  }
}
