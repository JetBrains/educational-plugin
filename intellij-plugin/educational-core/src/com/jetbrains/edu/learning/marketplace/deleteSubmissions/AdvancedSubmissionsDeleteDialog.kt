package com.jetbrains.edu.learning.marketplace.deleteSubmissions

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent

internal class AdvancedSubmissionsDeleteDialog(private val project: Project) : DialogWrapper(project), SubmissionsDeleteDialog {

  init {
    init()
    setOKButtonText(CommonBundle.message("button.delete"))
    isResizable = false
    title = EduCoreBundle.message("marketplace.delete.submissions.advanced.dialog.title")
  }

  private var checkBoxSelected: Boolean = false

  override fun createCenterPanel(): JComponent = panel {
    row {
      icon(AllIcons.General.WarningDialog)
      text(EduCoreBundle.message("marketplace.delete.submissions.advanced.dialog.text", project.course?.name ?: ""))
    }.bottomGap(BottomGap.MEDIUM)
    row {
      checkBox(EduCoreBundle.message("marketplace.delete.submissions.advanced.dialog.checkbox.text")).comment(
        EduCoreBundle.message(
          "marketplace.delete.submissions.advanced.dialog.checkbox.comment"
        )
      ).onChanged { checkBoxSelected = it.isSelected }
    }
  }

  override fun showWithResult(): Int {
    if (!super.showAndGet()) return CANCEL

    return if (checkBoxSelected) ALL else COURSE
  }

  companion object {
    const val COURSE: Int = 0
    const val ALL: Int = 1
    const val CANCEL: Int = 2

    fun showConfirmationDialog(project: Project): Int = if (isUnitTestMode) {
      testDialog?.showWithResult() ?: error("No test dialog specified")
    }
    else {
      AdvancedSubmissionsDeleteDialog(project).showWithResult()
    }

    var testDialog: SubmissionsDeleteDialog? = null
  }
}
