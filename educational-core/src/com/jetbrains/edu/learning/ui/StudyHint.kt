package com.jetbrains.edu.learning.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.CCEditHintAction
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.StudyUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.ui.taskDescription.JavaFxToolWindow
import com.jetbrains.edu.learning.ui.taskDescription.SwingToolWindow
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow
import java.util.*

open class StudyHint(private val myPlaceholder: AnswerPlaceholder?,
                     private val myProject: Project) {

  companion object {
    private val OUR_WARNING_MESSAGE = "Put the caret in the answer placeholder to get hint"
    private val HINTS_NOT_AVAILABLE = "There is no hint for this answer placeholder"
  }

  val taskDescriptionToolWindow: TaskDescriptionToolWindow
  protected var myShownHintNumber = 0
  protected var isEditingMode = false

  init {
    if (StudyUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) {
      taskDescriptionToolWindow = JavaFxToolWindow()
    }
    else {
      taskDescriptionToolWindow = SwingToolWindow()
    }
    taskDescriptionToolWindow.init(myProject, false)

    if (myPlaceholder == null) {
      taskDescriptionToolWindow.setText(OUR_WARNING_MESSAGE)
      taskDescriptionToolWindow.setActionToolbar(DefaultActionGroup())
    }

    val course = StudyTaskManager.getInstance(myProject).course
    if (course != null) {
      val group = DefaultActionGroup()
      val hints = myPlaceholder?.hints
      if (hints != null) {
        group.addAll(Arrays.asList(GoBackward(), GoForward(), CCEditHintAction(myPlaceholder)))
        taskDescriptionToolWindow.setActionToolbar(group)
        setHintText(hints)
      }
    }
  }

  protected fun setHintText(hints: List<String>) {
    if (!hints.isEmpty()) {
      taskDescriptionToolWindow.setText(hints[myShownHintNumber])
    }
    else {
      myShownHintNumber = -1
      taskDescriptionToolWindow.setText(HINTS_NOT_AVAILABLE)
    }
  }

  inner class GoForward : AnAction("Next Hint", "Next Hint", AllIcons.Actions.Forward) {


    override fun actionPerformed(e: AnActionEvent) {
      taskDescriptionToolWindow.setText(myPlaceholder!!.hints[++myShownHintNumber])
    }

    override fun update(e: AnActionEvent) {
      val presentation = e.presentation
      updateVisibility(myPlaceholder, presentation)
      presentation.isEnabled = !isEditingMode && myPlaceholder != null && myShownHintNumber + 1 < myPlaceholder.hints.size
    }
  }

  private fun updateVisibility(myPlaceholder: AnswerPlaceholder?,
                            presentation: Presentation) {
    val hasMultipleHints = myPlaceholder != null && myPlaceholder.hints.size > 1
    presentation.isVisible = !StudyUtils.isStudentProject(myProject) || hasMultipleHints
  }

  inner class GoBackward : AnAction("Previous Hint", "Previous Hint", AllIcons.Actions.Back) {

    override fun actionPerformed(e: AnActionEvent) {
      taskDescriptionToolWindow.setText(myPlaceholder!!.hints[--myShownHintNumber])
    }

    override fun update(e: AnActionEvent) {
      val presentation = e.presentation
      updateVisibility(myPlaceholder, presentation)
      presentation.isEnabled = !isEditingMode && myShownHintNumber - 1 >= 0
    }
  }
}
