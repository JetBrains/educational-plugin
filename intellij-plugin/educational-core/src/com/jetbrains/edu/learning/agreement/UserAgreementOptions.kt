package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
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
  private var pluginAgreement = userAgreementSettings.pluginAgreement
  private var aiFeaturesAgreement = userAgreementSettings.aiServiceAgreement
  private var submissionsServiceAgreement = userAgreementSettings.submissionsServiceAgreement
  private var solutionSharingPreference = userAgreementSettings.solutionSharing

  private lateinit var pluginAgreementCheckBox: Cell<JBCheckBox>
  private lateinit var aiFeaturesAgreementCheckBox: Cell<JBCheckBox>
  private lateinit var submissionsServiceCheckBox: Cell<JBCheckBox>
  private lateinit var solutionSharingPreferenceCheckBox: Cell<JBCheckBox>

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row {
        text(EduCoreBundle.message("user.agreement.dialog.text"))
      }
      row {
        pluginAgreementCheckBox = checkBox(EMPTY_TEXT)
          .bindSelected(::pluginAgreement)
          .enabled(!userAgreementSettings.isNotShown)
          .onChanged {
            if (!it.isSelected) {
              aiFeaturesAgreementCheckBox.component.isSelected = false
              submissionsServiceCheckBox.component.isSelected = false
              solutionSharingPreferenceCheckBox.component.isSelected = false
            }
          }
          .customize(UnscaledGaps.EMPTY)
        cell(createPluginAgreementCheckBoxTextPanel())
      }.customize(UnscaledGapsY(bottom = 5))
      indent {
        row {
          aiFeaturesAgreementCheckBox = checkBox(EMPTY_TEXT)
            .bindSelected(::aiFeaturesAgreement)
            .enabledIf(pluginAgreementCheckBox.selected)
            .customize(UnscaledGaps.EMPTY)
            .align(AlignY.TOP)
          cell(createAiAgreementCheckBoxTextPanel())
        }
        if (!RemoteEnvHelper.isRemoteDevServer()) {
          row {
            submissionsServiceCheckBox = checkBox(EduCoreBundle.message("marketplace.options.user.agreement.checkbox"))
              .bindSelected(::submissionsServiceAgreement)
              .enabledIf(pluginAgreementCheckBox.selected)
              .onChanged {
                if (!it.isSelected) {
                  solutionSharingPreferenceCheckBox.component.isSelected = false
                }
              }
          }
        }
        row {
          solutionSharingPreferenceCheckBox = checkBox(EduCoreBundle.message("marketplace.options.solutions.sharing.checkbox"))
            .bindSelected(::solutionSharingPreference)
            .enabledIf(submissionsServiceCheckBox.selected)
        }
      }
    }
  }

  override fun isModified(): Boolean {
    return super<BoundConfigurable>.isModified()
           || pluginAgreement != userAgreementSettings.pluginAgreement
           || aiFeaturesAgreement != userAgreementSettings.aiServiceAgreement
           || submissionsServiceAgreement != userAgreementSettings.submissionsServiceAgreement
           || solutionSharingPreference != userAgreementSettings.solutionSharing
  }

  override fun apply() {
    super.apply()
    if (!isModified) return

    val pluginAgreementState =
      if (pluginAgreement) UserAgreementState.ACCEPTED else UserAgreementState.TERMINATED
    val aiServiceAgreementState =
      if (aiFeaturesAgreement && pluginAgreement) UserAgreementState.ACCEPTED else UserAgreementState.TERMINATED
    val submissionsServiceAgreementState =
      if (submissionsServiceAgreement && pluginAgreement) UserAgreementState.ACCEPTED else UserAgreementState.TERMINATED
    val solutionSharingPreferenceState =
      if (solutionSharingPreference && submissionsServiceAgreement && pluginAgreement) SolutionSharingPreference.ALWAYS else SolutionSharingPreference.NEVER
    userAgreementSettings.updatePluginAgreementState(
      pluginAgreementState,
      aiServiceAgreementState,
      submissionsServiceAgreementState,
      solutionSharingPreferenceState
    )
  }
}