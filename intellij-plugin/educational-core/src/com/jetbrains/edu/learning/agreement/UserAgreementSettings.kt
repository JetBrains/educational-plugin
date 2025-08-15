package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.components.*
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.submissions.UserAgreementState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.annotations.TestOnly

@State(name = "UserAgreementSettings", storages = [Storage("edu.agreement.xml", roamingType = RoamingType.DEFAULT)])
class UserAgreementSettings : PersistentStateComponent<UserAgreementSettings.State>, EduTestAware {
  private val _userAgreementProperties = MutableStateFlow(UserAgreementProperties())
  val userAgreementProperties: StateFlow<UserAgreementProperties> = _userAgreementProperties.asStateFlow()

  val isPluginAllowed: Boolean
    get() {
      val pluginAgreement = _userAgreementProperties.value.pluginAgreement
      return pluginAgreement == UserAgreementState.ACCEPTED || pluginAgreement == UserAgreementState.NOT_SHOWN
    }

  val pluginAgreement: Boolean
    get() = _userAgreementProperties.value.pluginAgreement == UserAgreementState.ACCEPTED

  val aiServiceAgreement: Boolean
    get() = _userAgreementProperties.value.aiServiceAgreement == UserAgreementState.ACCEPTED

  val solutionSharing: Boolean
    get() = _userAgreementProperties.value.solutionSharingPreference == SolutionSharingPreference.ALWAYS

  fun setSolutionSharing(solutionSharingPreference: SolutionSharingPreference = SolutionSharingPreference.ALWAYS) {
    _userAgreementProperties.value = _userAgreementProperties.value.copy(solutionSharingPreference = solutionSharingPreference)
  }

  fun setAgreementState(agreementState: AgreementStateResponse) {
    _userAgreementProperties.value = UserAgreementProperties(
      pluginAgreement = agreementState.pluginAgreement,
      aiServiceAgreement = agreementState.aiAgreement,
      isChangedByUser = true
    )
  }

  data class AgreementStateResponse(
    val pluginAgreement: UserAgreementState = UserAgreementState.DECLINED,
    val aiAgreement: UserAgreementState = UserAgreementState.DECLINED
  )

  fun updatePluginAgreementState(
    pluginAgreement: UserAgreementState,
    aiServiceAgreement: UserAgreementState,
    solutionSharingPreference: SolutionSharingPreference
  ) {
    _userAgreementProperties.value = UserAgreementProperties(
      pluginAgreement = pluginAgreement,
      aiServiceAgreement = aiServiceAgreement,
      solutionSharingPreference = solutionSharingPreference,
      isChangedByUser = true
    )
  }

  fun resetUserAgreementSettings() {
    _userAgreementProperties.value = UserAgreementProperties(isChangedByUser = true)
  }

  data class UserAgreementProperties(
    val pluginAgreement: UserAgreementState = UserAgreementState.NOT_SHOWN,
    val aiServiceAgreement: UserAgreementState = UserAgreementState.NOT_SHOWN,
    val solutionSharingPreference: SolutionSharingPreference = SolutionSharingPreference.NEVER,
    val isChangedByUser: Boolean = false
  )

  class State : BaseState() {
    var pluginAgreement by enum<UserAgreementState>(UserAgreementState.NOT_SHOWN)
    var aiServiceAgreement by enum<UserAgreementState>(UserAgreementState.NOT_SHOWN)
    var solutionSharingPreference by enum<SolutionSharingPreference>(SolutionSharingPreference.NEVER)
  }

  override fun getState(): State {
    val state = State()
    state.pluginAgreement = _userAgreementProperties.value.pluginAgreement
    state.aiServiceAgreement = _userAgreementProperties.value.aiServiceAgreement
    state.solutionSharingPreference = _userAgreementProperties.value.solutionSharingPreference
    return state
  }

  override fun loadState(state: State) {
    _userAgreementProperties.value = UserAgreementProperties(
      state.pluginAgreement,
      state.aiServiceAgreement,
      state.solutionSharingPreference
    )
  }

  @TestOnly
  override fun restoreState() {
    _userAgreementProperties.value = UserAgreementProperties()
  }

  companion object {
    fun getInstance(): UserAgreementSettings = service()
  }
}