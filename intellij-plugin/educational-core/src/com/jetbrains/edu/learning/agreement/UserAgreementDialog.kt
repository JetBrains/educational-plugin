package com.jetbrains.edu.learning.agreement

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.EMPTY_TEXT
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.createAiAgreementCheckBoxTextPanel
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.createPluginAgreementCheckBoxTextPanel
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.UserAgreementState
import javax.swing.JComponent

class UserAgreementDialog(project: Project?) : DialogWrapper(project) {
  init {
    setOKButtonText(EduCoreBundle.message("user.agreement.dialog.agree.button"))
    isResizable = false
    isOKActionEnabled = false
    title = EduCoreBundle.message("user.agreement.dialog.title")
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row {
      icon(AllIcons.General.QuestionDialog).align(AlignY.TOP)
      cell(createInnerPanel())
    }
  }.apply { border = JBUI.Borders.empty(5) }

  private lateinit var pluginAgreementCheckBox: Cell<JBCheckBox>
  private lateinit var aiAgreementCheckBox: Cell<JBCheckBox>

  private fun createInnerPanel(): JComponent = panel {
    row {
      text(EduCoreBundle.message("user.agreement.dialog.text"))
    }
    row {
      pluginAgreementCheckBox = checkBox(EMPTY_TEXT)
        .comment(EduCoreBundle.message("user.agreement.dialog.plugin.agreement.checkbox.comment"))
        .onChanged {
          if (!it.isSelected) {
            aiAgreementCheckBox.selected(false)
          }
          isOKActionEnabled = it.isSelected
        }
        .customize(UnscaledGaps.EMPTY)
      cell(createPluginAgreementCheckBoxTextPanel())
    }
    row {
      aiAgreementCheckBox = checkBox(EMPTY_TEXT)
        .enabledIf(pluginAgreementCheckBox.selected)
        .comment(EduCoreBundle.message("user.agreement.dialog.ai.agreement.checkbox.comment"))
        .customize(UnscaledGaps.EMPTY)
        .align(AlignY.TOP)
      cell(createAiAgreementCheckBoxTextPanel())
    }
  }

  fun showWithResult(): UserAgreementSettings.AgreementStateResponse {
    val result = showAndGet()
    if (!result) {
      return UserAgreementSettings.AgreementStateResponse()
    }
    val pluginAgreementState =
      if (pluginAgreementCheckBox.component.isSelected) UserAgreementState.ACCEPTED else UserAgreementState.DECLINED
    val aiAgreementState =
      if (aiAgreementCheckBox.component.isSelected) UserAgreementState.ACCEPTED else UserAgreementState.DECLINED
    return UserAgreementSettings.AgreementStateResponse(pluginAgreement = pluginAgreementState, aiAgreement = aiAgreementState)
  }

  companion object {
    @RequiresEdt
    fun showUserAgreementDialog(project: Project?): Boolean {
      val result = UserAgreementDialog(project).showWithResult()
      UserAgreementSettings.getInstance().setAgreementState(result)
      val isAccepted = result.pluginAgreement == UserAgreementState.ACCEPTED
      return isAccepted
    }

    fun showEnableSubmissionsDialog(project: Project): Boolean {
      val result = showYesNoDialog(
        EduCoreBundle.message("user.agreement.settings.title"),
        EduCoreBundle.message("marketplace.options.user.agreement.checkbox"),
        project
      )
      if (result) {
        SubmissionsManager.getInstance(project).prepareSubmissionsContentWhenLoggedIn()
      }
      return result
    }
  }
}