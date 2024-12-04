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
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.EMPTY_TEXT
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.createAiAgreementCheckBoxTextPanel
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.createPluginAgreementCheckBoxTextPanel
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.submissions.UserAgreementState

class UserAgreementOptions : BoundConfigurable(EduCoreBundle.message("user.agreement.settings.title")), OptionsProvider {
  private val userAgreementSettings = UserAgreementSettings.getInstance()

  private val pluginAgreementAccepted: MutableBooleanProperty = AtomicBooleanProperty(userAgreementSettings.pluginAgreement)
  private val aiAgreementAccepted: MutableBooleanProperty = AtomicBooleanProperty(userAgreementSettings.aiServiceAgreement)
  private val submissionsServiceAccepted: MutableBooleanProperty = AtomicBooleanProperty(userAgreementSettings.submissionsServiceAgreement)
  private val solutionSharingAccepted: MutableBooleanProperty = AtomicBooleanProperty(userAgreementSettings.solutionSharing)

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row {
        text(EduCoreBundle.message("user.agreement.dialog.text"))
      }
      row {
        checkBox(EMPTY_TEXT)
          .bindSelected(pluginAgreementAccepted)
          .enabled(!userAgreementSettings.isNotShown)
          .onChanged {
            if (!it.isSelected) {
              aiAgreementAccepted.set(false)
              submissionsServiceAccepted.set(false)
              solutionSharingAccepted.set(false)
            }
          }
          .customize(UnscaledGaps.EMPTY)
        cell(createPluginAgreementCheckBoxTextPanel())
      }.customize(UnscaledGapsY(bottom = 5))
      indent {
        row {
          checkBox(EMPTY_TEXT)
            .bindSelected(aiAgreementAccepted)
            .enabledIf(pluginAgreementAccepted)
            .customize(UnscaledGaps.EMPTY)
            .align(AlignY.TOP)
          cell(createAiAgreementCheckBoxTextPanel())
        }
        if (!RemoteEnvHelper.isRemoteDevServer()) {
          row {
            checkBox(EduCoreBundle.message("marketplace.options.user.agreement.checkbox"))
              .bindSelected(submissionsServiceAccepted)
              .enabledIf(pluginAgreementAccepted)
              .onChanged {
                if (!it.isSelected) {
                  solutionSharingAccepted.set(false)
                }
              }
          }
        }
        row {
          checkBox(EduCoreBundle.message("marketplace.options.solutions.sharing.checkbox"))
            .bindSelected(solutionSharingAccepted)
            .enabledIf(submissionsServiceAccepted)
        }
      }
    }
  }

  override fun isModified(): Boolean {
    return super<BoundConfigurable>.isModified()
           || pluginAgreementAccepted.get() != userAgreementSettings.pluginAgreement
           || aiAgreementAccepted.get() != userAgreementSettings.aiServiceAgreement
           || submissionsServiceAccepted.get() != userAgreementSettings.submissionsServiceAgreement
           || solutionSharingAccepted.get() != userAgreementSettings.solutionSharing
  }

  override fun apply() {
    super.apply()
    if (!isModified) return

    val pluginAgreementAccepted = pluginAgreementAccepted.get()
    val submissionsServiceAccepted = submissionsServiceAccepted.get()
    val solutionSharingAccepted = solutionSharingAccepted.get()
    val pluginAgreementState =
      if (pluginAgreementAccepted) UserAgreementState.ACCEPTED else UserAgreementState.TERMINATED
    val aiServiceAgreementState =
      if (aiAgreementAccepted.get() && pluginAgreementAccepted) UserAgreementState.ACCEPTED else UserAgreementState.TERMINATED
    val submissionsServiceAgreementState =
      if (solutionSharingAccepted && pluginAgreementAccepted) UserAgreementState.ACCEPTED else UserAgreementState.TERMINATED
    val solutionSharingPreference = if (solutionSharingAccepted && submissionsServiceAccepted && pluginAgreementAccepted)
      SolutionSharingPreference.ALWAYS else SolutionSharingPreference.NEVER

    userAgreementSettings.updatePluginAgreementState(
      pluginAgreementState,
      aiServiceAgreementState,
      submissionsServiceAgreementState,
      solutionSharingPreference
    )
  }
}