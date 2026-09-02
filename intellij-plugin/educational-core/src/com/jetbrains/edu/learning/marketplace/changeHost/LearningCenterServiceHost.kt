package com.jetbrains.edu.learning.marketplace.changeHost

import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostEnum
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.marketplace.LEARNING_CENTER_PRODUCTION_URL
import com.jetbrains.edu.learning.marketplace.LEARNING_CENTER_STAGING_URL
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.PropertyKey

@Suppress("unused") // All enum values ar used in UI
enum class LearningCenterServiceHost(
  override val url: String,
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String
) : ServiceHostEnum {
  PRODUCTION(LEARNING_CENTER_PRODUCTION_URL, "change.service.host.production"),
  STAGING(LEARNING_CENTER_STAGING_URL, "change.service.host.staging"),
  OTHER("http://localhost:8080", "change.service.host.other");

  override fun visibleName(): @NlsContexts.ListItem String = EduCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<LearningCenterServiceHost>("Learning Center", LearningCenterServiceHost::class.java) {

    override val default: LearningCenterServiceHost = PRODUCTION
    override val other: LearningCenterServiceHost = OTHER
  }
}
