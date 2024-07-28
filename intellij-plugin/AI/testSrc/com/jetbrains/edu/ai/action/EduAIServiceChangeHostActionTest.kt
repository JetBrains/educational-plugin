package com.jetbrains.edu.ai.action

import com.jetbrains.edu.ai.EDU_AI_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.learning.services.action.ServiceChangeHostActionTest
import org.jetbrains.annotations.NonNls

class EduAIServiceChangeHostActionTest : ServiceChangeHostActionTest() {
  override val actionId: String = EduAIServiceChangeHost.ACTION_ID
  override val propertyName: String = EDU_AI_SERVICE_HOST_PROPERTY
  override var initialUrl: String = EduAIServiceHost.PRODUCTION.url

  override val productionUrl: String = EduAIServiceHost.PRODUCTION.url
  override val stagingUrl: String = EduAIServiceHost.STAGING.url
  override val localhostUrl: String = LOCALHOST_URL

  override fun getSelectedUrl(): String = EduAIServiceHost.getSelectedUrl()

  companion object {
    @NonNls
    private const val LOCALHOST_URL = "http://localhost:666/"
  }
}