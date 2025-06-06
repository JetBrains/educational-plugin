package com.jetbrains.edu.learning.agreement

import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.MutableBooleanProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.aiAgreementCheckBoxText
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.pluginAgreementCheckBoxText
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.UserAgreementState
import javax.swing.JComponent

class UserAgreementDialog(project: Project) : DialogWrapper(project) {
  private val pluginAgreementAccepted: MutableBooleanProperty = AtomicBooleanProperty(UserAgreementSettings.getInstance().pluginAgreement)

  private val aiAgreementCheckBox: MutableBooleanProperty = AtomicBooleanProperty(UserAgreementSettings.getInstance().aiServiceAgreement)

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

  private fun createInnerPanel(): JComponent = panel {
    row {
      text(EduCoreBundle.message("user.agreement.dialog.text"))
    }
    row {
      checkBox("")
        .comment(EduCoreBundle.message("user.agreement.dialog.plugin.agreement.checkbox.comment"))
        .onChanged {
          if (!it.isSelected) {
            aiAgreementCheckBox.set(false)
          }
          isOKActionEnabled = it.isSelected
        }
        .bindSelected(pluginAgreementAccepted)
        .customize(UnscaledGaps.EMPTY)
      pluginAgreementCheckBoxText()
    }
    row {
      checkBox("")
        .enabledIf(pluginAgreementAccepted)
        .comment(EduCoreBundle.message("user.agreement.dialog.ai.agreement.checkbox.comment"))
        .customize(UnscaledGaps.EMPTY)
        .align(AlignY.TOP)
        .bindSelected(aiAgreementCheckBox)
      aiAgreementCheckBoxText()
    }
  }

  fun showWithResult(): UserAgreementSettings.AgreementStateResponse {
    val result = showAndGet()
    if (!result) {
      return UserAgreementSettings.AgreementStateResponse(
        pluginAgreement = if (UserAgreementSettings.getInstance().pluginAgreement) UserAgreementState.ACCEPTED else UserAgreementState.DECLINED,
        aiAgreement = if (UserAgreementSettings.getInstance().aiServiceAgreement) UserAgreementState.ACCEPTED else UserAgreementState.DECLINED
      )
    }
    val pluginAgreementState =
      if (pluginAgreementAccepted.get()) UserAgreementState.ACCEPTED else UserAgreementState.DECLINED
    val aiAgreementState =
      if (aiAgreementCheckBox.get()) UserAgreementState.ACCEPTED else UserAgreementState.DECLINED
    return UserAgreementSettings.AgreementStateResponse(pluginAgreement = pluginAgreementState, aiAgreement = aiAgreementState)
  }
}