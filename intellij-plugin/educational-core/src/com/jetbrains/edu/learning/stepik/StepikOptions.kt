package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduExperimentalFeatures.CC_HYPERSKILL
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.settings.OAuthLoginOptions
import com.jetbrains.edu.learning.stepik.api.StepikConnector

class StepikOptions : OAuthLoginOptions<StepikUser>() {
  override val connector: EduOAuthCodeFlowConnector<StepikUser, *>
    get() = StepikConnector.getInstance()

  override fun getDisplayName(): String = StepikNames.STEPIK

  override fun profileUrl(account: StepikUser): String = account.profileUrl

  override fun isAvailable(): Boolean = super.isAvailable() && isFeatureEnabled(CC_HYPERSKILL)
}
