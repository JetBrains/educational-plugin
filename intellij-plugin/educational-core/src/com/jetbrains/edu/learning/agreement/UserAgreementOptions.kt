package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.MutableBooleanProperty
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.ui.dsl.gridLayout.UnscaledGapsY
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.UserAgreementProperties
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.aiAgreementCheckBoxText
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.pluginAgreementCheckBoxText
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.submissions.UserAgreementState

class UserAgreementOptions : BoundConfigurable(EduCoreBundle.message("user.agreement.settings.title")), OptionsProvider {
  private val userAgreementSettings = UserAgreementSettings.getInstance()

  private val pluginAgreementAccepted: MutableBooleanProperty = AtomicBooleanProperty(userAgreementSettings.pluginAgreement)
  private val aiAgreementAccepted: MutableBooleanProperty = AtomicBooleanProperty(userAgreementSettings.aiServiceAgreement)
  private val solutionSharingAccepted: MutableBooleanProperty = AtomicBooleanProperty(userAgreementSettings.solutionSharing)

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row {
        text(EduCoreBundle.message("user.agreement.dialog.text"))
      }
      row {
        checkBox("")
          .bindSelected(pluginAgreementAccepted)
          .onChanged {
            if (!it.isSelected) {
              aiAgreementAccepted.set(false)
              solutionSharingAccepted.set(false)
            }
          }
          .customize(UnscaledGaps.EMPTY)
        pluginAgreementCheckBoxText()
      }.customize(UnscaledGapsY(bottom = 5))
      indent {
        row {
          checkBox("")
            .bindSelected(aiAgreementAccepted)
            .enabledIf(pluginAgreementAccepted)
            .customize(UnscaledGaps.EMPTY)
            .align(AlignY.TOP)
          aiAgreementCheckBoxText()
        }
        row {
          checkBox(EduCoreBundle.message("marketplace.options.solutions.sharing.checkbox"))
            .bindSelected(solutionSharingAccepted)
            .enabledIf(pluginAgreementAccepted)
        }
      }
    }
  }

  override fun isModified(): Boolean {
    return super<BoundConfigurable>.isModified()
           || pluginAgreementAccepted.get() != userAgreementSettings.pluginAgreement
           || aiAgreementAccepted.get() != userAgreementSettings.aiServiceAgreement
           || solutionSharingAccepted.get() != userAgreementSettings.solutionSharing
  }

  override fun apply() {
    super.apply()
    if (!isModified) return

    val pluginAgreementAccepted = pluginAgreementAccepted.get()
    val solutionSharingAccepted = solutionSharingAccepted.get()
    val pluginAgreementState =
      if (pluginAgreementAccepted) UserAgreementState.ACCEPTED else UserAgreementState.TERMINATED
    val aiServiceAgreementState =
      if (aiAgreementAccepted.get() && pluginAgreementAccepted) UserAgreementState.ACCEPTED else UserAgreementState.TERMINATED
    val solutionSharingPreference = if (solutionSharingAccepted && pluginAgreementAccepted)
      SolutionSharingPreference.ALWAYS else SolutionSharingPreference.NEVER

    userAgreementSettings.updatePluginAgreementState(
      UserAgreementProperties(pluginAgreementState, aiServiceAgreementState, solutionSharingPreference)
    )
  }
}