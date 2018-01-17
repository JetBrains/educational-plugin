
package com.jetbrains.edu.coursecreator.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.placeholder.CCCreateAnswerPlaceholderDialog
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

class CCEditHintAction(private val myPlaceholder: AnswerPlaceholder?) : AnAction("Edit Hint", "Edit Hint", AllIcons.Modules.Edit) {

  override fun actionPerformed(e: AnActionEvent) {
    val dlg = CCCreateAnswerPlaceholderDialog(e.project!!, myPlaceholder!!.placeholderText, myPlaceholder.hints)
    dlg.title = "Edit Answer Placeholder"
    if (dlg.showAndGet()) {
      val answerPlaceholderText = dlg.taskText
      myPlaceholder.placeholderText = answerPlaceholderText
      myPlaceholder.length = StringUtil.notNullize(answerPlaceholderText).length
      myPlaceholder.hints = dlg.hints
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCUtils.isCourseCreator(e.project!!) && myPlaceholder != null
  }
}
