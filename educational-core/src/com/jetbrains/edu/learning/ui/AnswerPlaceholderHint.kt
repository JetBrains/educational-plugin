package com.jetbrains.edu.learning.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.CCEditHintAction
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import java.util.*

open class AnswerPlaceholderHint(private val myPlaceholder: AnswerPlaceholder?,
                                 private val myProject: Project) {

  companion object {
    val NO_PLACEHOLDER_MESSAGE = "Put the caret in the answer placeholder to get hint"
    val HINTS_NOT_AVAILABLE = "There is no hint for this answer placeholder"
  }

  val hintComponent: HintComponent = HintComponent()

  protected var myShownHintNumber = 0
  protected var isEditingMode = false

  init {
    if (myPlaceholder == null) {
      hintComponent.text = NO_PLACEHOLDER_MESSAGE
      hintComponent.setActionToolbar(DefaultActionGroup())
    }

    val course = StudyTaskManager.getInstance(myProject).course
    if (course != null) {
      val group = DefaultActionGroup()
      val hints = myPlaceholder?.hints
      if (hints != null) {
        group.addAll(Arrays.asList(GoBackward(), GoForward(), CCEditHintAction(myPlaceholder)))
        hintComponent.setActionToolbar(group)
        setHintText(hints)
      }
    }
  }

  private fun setHintText(hints: List<String>) = if (!hints.isEmpty()) {
    hintComponent.setText(hints[myShownHintNumber])
  } else {
    myShownHintNumber = -1
    hintComponent.setText(HINTS_NOT_AVAILABLE)
  }

  fun getGoForwardAction(): GoForward = GoForward()
  fun getGoBackwardAction(): GoBackward = GoBackward()

  inner class GoForward : AnAction("Next Hint", "Next Hint", AllIcons.Actions.Forward) {

    override fun actionPerformed(e: AnActionEvent) {
      hintComponent.text = myPlaceholder!!.hints[++myShownHintNumber]
    }

    override fun update(e: AnActionEvent) {
      val presentation = e.presentation
      updateVisibility(myPlaceholder, presentation)
      presentation.isEnabled = !isEditingMode && myPlaceholder != null && myShownHintNumber + 1 < myPlaceholder.hints.size
    }
  }

  private fun updateVisibility(myPlaceholder: AnswerPlaceholder?, presentation: Presentation) {
    val hasMultipleHints = myPlaceholder != null && myPlaceholder.hints.size > 1
    presentation.isVisible = !EduUtils.isStudentProject(myProject) || hasMultipleHints
  }

  inner class GoBackward : AnAction("Previous Hint", "Previous Hint", AllIcons.Actions.Back) {

    override fun actionPerformed(e: AnActionEvent) {
      hintComponent.text = myPlaceholder!!.hints[--myShownHintNumber]
    }

    override fun update(e: AnActionEvent) {
      val presentation = e.presentation
      updateVisibility(myPlaceholder, presentation)
      presentation.isEnabled = !isEditingMode && myShownHintNumber - 1 >= 0
    }
  }
}
