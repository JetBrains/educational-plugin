package com.jetbrains.edu.lti.changeHost

import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostEnum
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.PropertyKey

@Suppress("unused") // All enum values ar used in UI
enum class LTIServiceHost(
  override val url: String,
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String
) : ServiceHostEnum {
  PRODUCTION("https://lti-tool-production.labs.jb.gg/", "change.service.host.production"),
  STAGING("https://lti-tool-staging.labs.jb.gg/", "change.service.host.staging"),
  OTHER("http://localhost:8083", "change.service.host.other");

  override fun visibleName(): @NlsContexts.ListItem String = EduCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<LTIServiceHost>("Submissions service", LTIServiceHost::class.java) {
    override val default: LTIServiceHost = PRODUCTION
    override val other: LTIServiceHost = OTHER
  }
}
