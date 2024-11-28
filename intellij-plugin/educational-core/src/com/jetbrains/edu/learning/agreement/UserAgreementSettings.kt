package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.components.*
import com.intellij.util.application
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.submissions.UserAgreementState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

fun userAgreementSettings(): UserAgreementSettings = application.service()

@Service
@State(name = "UserAgreementSettings", storages = [Storage("edu.agreement.xml", roamingType = RoamingType.DEFAULT)])
class UserAgreementSettings : PersistentStateComponent<UserAgreementSettings.State> {
  private val _userAgreementProperties = MutableStateFlow(UserAgreementProperties())
  val userAgreementProperties = _userAgreementProperties.asStateFlow()

  val isPluginAllowed: Boolean
    get() {
      val pluginAgreement = _userAgreementProperties.value.pluginAgreement
      return pluginAgreement == UserAgreementState.ACCEPTED || pluginAgreement == UserAgreementState.NOT_SHOWN
    }

  val pluginAgreement: Boolean
    get() = _userAgreementProperties.value.pluginAgreement == UserAgreementState.ACCEPTED

  val submissionsServiceAgreement: Boolean
    get() = _userAgreementProperties.value.submissionsServiceAgreement == UserAgreementState.ACCEPTED

  val aiServiceAgreement: Boolean
    get() = _userAgreementProperties.value.aiServiceAgreement == UserAgreementState.ACCEPTED

  val solutionSharing: Boolean
    get() = _userAgreementProperties.value.solutionSharing == SolutionSharingPreference.ALWAYS

  val isNotShown: Boolean
    get() = _userAgreementProperties.value.pluginAgreement == UserAgreementState.NOT_SHOWN

  fun enableSubmissions() {
    _userAgreementProperties.value = _userAgreementProperties.value.copy(submissionsServiceAgreement = UserAgreementState.ACCEPTED)
  }

  fun setAgreementState(agreementState: AgreementStateResponse) {
    _userAgreementProperties.value = UserAgreementProperties(
      pluginAgreement = agreementState.pluginAgreement,
      aiServiceAgreement = agreementState.aiAgreement,
      submissionsServiceAgreement = agreementState.pluginAgreement
    )
  }

  data class AgreementStateResponse(
    val pluginAgreement: UserAgreementState = UserAgreementState.DECLINED,
    val aiAgreement: UserAgreementState = UserAgreementState.DECLINED
  )

  fun updatePluginAgreementState(
    pluginAgreement: UserAgreementState,
    aiServiceAgreement: UserAgreementState,
    submissionsServiceAgreement: UserAgreementState,
    solutionSharingPreference: SolutionSharingPreference
  ) {
    _userAgreementProperties.value = UserAgreementProperties(
      pluginAgreement = pluginAgreement,
      aiServiceAgreement = aiServiceAgreement,
      submissionsServiceAgreement = submissionsServiceAgreement,
      solutionSharing = solutionSharingPreference
    )
  }

  fun resetUserAgreementSettings() {
    _userAgreementProperties.value = UserAgreementProperties()
  }

  data class UserAgreementProperties(
    val pluginAgreement: UserAgreementState = UserAgreementState.NOT_SHOWN,
    val submissionsServiceAgreement: UserAgreementState = UserAgreementState.NOT_SHOWN,
    val aiServiceAgreement: UserAgreementState = UserAgreementState.NOT_SHOWN,
    val solutionSharing: SolutionSharingPreference = SolutionSharingPreference.NEVER
  )

  class State : BaseState() {
    var pluginAgreement by enum<UserAgreementState>(UserAgreementState.NOT_SHOWN)
    var submissionsServiceAgreement by enum<UserAgreementState>(UserAgreementState.NOT_SHOWN)
    var aiServiceAgreement by enum<UserAgreementState>(UserAgreementState.NOT_SHOWN)
    var solutionSharing by enum<SolutionSharingPreference>(SolutionSharingPreference.NEVER)
  }

  override fun getState(): State {
    val state = State()
    state.pluginAgreement = _userAgreementProperties.value.pluginAgreement
    state.submissionsServiceAgreement = _userAgreementProperties.value.submissionsServiceAgreement
    state.aiServiceAgreement = _userAgreementProperties.value.aiServiceAgreement
    state.solutionSharing = _userAgreementProperties.value.solutionSharing
    return state
  }

  override fun loadState(state: State) {
    _userAgreementProperties.value = UserAgreementProperties(
      state.pluginAgreement,
      state.submissionsServiceAgreement,
      state.aiServiceAgreement,
      state.solutionSharing
    )
  }
}