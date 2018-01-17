package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.placeholder.CCEditAnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

class CCEditHintAction(private val myPlaceholder: AnswerPlaceholder?) : AnAction("Edit Hint", "Edit Hint", AllIcons.Modules.Edit) {

  override fun actionPerformed(e: AnActionEvent?) {
    val project = e?.project ?: return
    if (myPlaceholder == null) {
      return
    }
    CCEditAnswerPlaceholder.performEditPlaceholder(project, myPlaceholder)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCUtils.isCourseCreator(e.project!!) && myPlaceholder != null
  }
}

