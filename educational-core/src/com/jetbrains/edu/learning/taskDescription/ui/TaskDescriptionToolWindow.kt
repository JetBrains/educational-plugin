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

import HintsWrapper
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.jetbrains.edu.learning.courseFormat.ext.getTaskTextFromTask
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.processImagesAndLinks
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.SwitchableHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.CodeHighlighter
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.ListenersAdder
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.MediaThemesAndExternalLinkIconsTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.ResourceWrapper
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.VideoTaskFilter
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.createViewerDependingOnCurrentUILibrary
import org.jsoup.nodes.Element
import javax.swing.JComponent

abstract class TaskDescriptionToolWindow(protected val project: Project) : Disposable {
  private val switchableHintsWrapper = SwitchableHtmlTransformer(HintsWrapper)

  private val taskDescriptionHtmlTransformationChain = VideoTaskFilter then
    MediaThemesAndExternalLinkIconsTransformer then
    CodeHighlighter then
    switchableHintsWrapper then
    ResourceWrapper then
    ListenersAdder

  private val taskDescriptionViewer = createViewerDependingOnCurrentUILibrary(project, taskDescriptionHtmlTransformationChain)

  init {
    Disposer.register(this, taskDescriptionViewer)
  }

  //default value of merging time span is 300 milliseconds, can be set in educational-core.xml
  @Suppress("LeakingThis")
  private val updateQueue = MergingUpdateQueue(TASK_DESCRIPTION_UPDATE,
                                               Registry.intValue(TASK_DESCRIPTION_UPDATE_DELAY_REGISTRY_KEY),
                                               true,
                                               null,
                                               this)

  open val taskInfoPanel: JComponent
    get() = taskDescriptionViewer.component

  abstract val taskSpecificPanel: JComponent

  open fun updateTaskSpecificPanel(task: Task?) {}

  protected abstract fun wrapHint(hintElement: Element, displayedHintNumber: String, hintTitle: String): String

  fun setTaskText(task: Task?) {
    updateQueue.queue(Update.create(TASK_DESCRIPTION_UPDATE) {
      setText(task)
    })
  }

  private fun setText(task: Task?) {
    switchableHintsWrapper.enabled = task !is TheoryTask
    val html = getTaskDescription(project, task)
    taskDescriptionViewer.setHtmlWithContext(html, HtmlTransformerContext(project, task))
  }

  private fun getTaskDescription(project: Project, task: Task?): String {
    if (task != null) {
      val taskText = task.getTaskTextFromTask(project)
      if (taskText != null) return taskText
    }
    return EduCoreBundle.message("label.open.task")
  }

  override fun dispose() {}

  companion object {
    private const val TASK_DESCRIPTION_UPDATE: String = "Task Description Update"
    const val TASK_DESCRIPTION_UPDATE_DELAY_REGISTRY_KEY: String = "edu.task.description.update.delay"

    @VisibleForTesting
    fun getTaskDescriptionWithCodeHighlighting(project: Project, task: Task?): String {
      if (task == null) return EduCoreBundle.message("label.open.task")
      val taskText = task.getTaskTextFromTask(project)
      if (taskText != null) {
        if (task is VideoTask) {
          return taskText
        }

        val processedText = processImagesAndLinks(project, task, taskText)

        val course = task.course
        val language = if (course is HyperskillCourse) PlainTextLanguage.INSTANCE else course.languageById ?: return processedText
        return EduCodeHighlighter.highlightCodeFragments(project, processedText, language)
      }
      return EduCoreBundle.message("label.open.task")
    }
  }
}
