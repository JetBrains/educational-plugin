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

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.SwitchableHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.*
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.ListenersAdder
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.CodeHighlighter
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.createViewerDependingOnCurrentUILibrary
import javax.swing.JComponent

@Suppress("LeakingThis")
abstract class TaskDescriptionToolWindow(protected val project: Project) : Disposable {
  private val switchableHintsWrapper = SwitchableHtmlTransformer(HintsWrapper)

  private val taskDescriptionHtmlTransformationChain = VideoTaskFilter then
    MediaThemesAndExternalLinkIconsTransformer then
    CodeHighlighter then
    switchableHintsWrapper then
    ResourceWrapper then
    ListenersAdder

  private val choiceOptionsHtmlTransformationChain = ChoiceTaskTransformer then
    MediaThemesAndExternalLinkIconsTransformer then
    CodeHighlighter then
    HintsWrapper then
    ResourceWrapper then
    ListenersAdder


  private val taskDescriptionViewer = createViewerDependingOnCurrentUILibrary(project, taskDescriptionHtmlTransformationChain)
  protected val taskSpecificPanelViewer = createViewerDependingOnCurrentUILibrary(project, choiceOptionsHtmlTransformationChain)

  init {
    Disposer.register(this, taskDescriptionViewer)
    Disposer.register(this, taskSpecificPanelViewer)
  }

  // The default value of merging time span is 300 milliseconds, can be set in educational-core.xml
  private val updateQueue = MergingUpdateQueue(TASK_DESCRIPTION_UPDATE,
                                               Registry.intValue(TASK_DESCRIPTION_UPDATE_DELAY_REGISTRY_KEY),
                                               true,
                                               null,
                                               this)

  open val taskInfoPanel: JComponent
    get() = taskDescriptionViewer.component

  open val taskSpecificPanel: JComponent
    get() = taskSpecificPanelViewer.component

  abstract fun updateTaskSpecificPanel(task: Task?)

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
      val taskText = EduUtils.getTaskTextFromTask(project, task)
      if (taskText != null) return taskText
    }
    return EduCoreBundle.message("label.open.task")
  }

  override fun dispose() {}

  companion object {
    private const val TASK_DESCRIPTION_UPDATE: String = "Task Description Update"
    const val TASK_DESCRIPTION_UPDATE_DELAY_REGISTRY_KEY: String = "edu.task.description.update.delay"
  }
}
