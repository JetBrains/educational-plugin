package com.jetbrains.edu.ai.action

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.ai.EDU_AI_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.services.dialog.ServiceHostChanger
import com.jetbrains.edu.learning.services.dialog.withMockServiceHostChanger
import com.jetbrains.edu.learning.testAction
import org.jetbrains.annotations.NonNls
import org.junit.Test

class EduAIServiceChangeHostActionTest : EduTestCase() {
  private var initialUrl: String = EduAIServiceHost.PRODUCTION.url

  override fun setUp() {
    super.setUp()
    initialUrl = EduAIServiceHost.getSelectedUrl()
  }

  override fun tearDown() {
    try {
      PropertiesComponent.getInstance().setValue(EDU_AI_SERVICE_HOST_PROPERTY, initialUrl, EduAIServiceHost.PRODUCTION.url)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test change production to staging host`() =
    doTestAIServiceChanged(EduAIServiceHost.PRODUCTION.url, EduAIServiceHost.STAGING.url)

  @Test
  fun `test change back from staging to production`() =
    doTestAIServiceChanged(EduAIServiceHost.STAGING.url, EduAIServiceHost.PRODUCTION.url)

  @Test
  fun `test change production to localhost`() =
    doTestAIServiceChanged(EduAIServiceHost.PRODUCTION.url, LOCALHOST_URL)

  @Test
  fun `test change back from localhost to production`() =
    doTestAIServiceChanged(LOCALHOST_URL, EduAIServiceHost.PRODUCTION.url)

  @Test
  fun `test change staging to localhost`() = doTestAIServiceChanged(EduAIServiceHost.STAGING.url, LOCALHOST_URL)

  @Test
  fun `test change back from localhost to staging`() = doTestAIServiceChanged(LOCALHOST_URL, EduAIServiceHost.STAGING.url)

  private fun doTestAIServiceChanged(initialHost: String, newHost: String) {
    PropertiesComponent.getInstance().setValue(EDU_AI_SERVICE_HOST_PROPERTY, initialHost)
    assertEquals(initialHost, EduAIServiceHost.getSelectedUrl())
    doAIServiceChangeHostAction(newHost)
    assertEquals(newHost, EduAIServiceHost.getSelectedUrl())
  }

  private fun doAIServiceChangeHostAction(url: String) {
    withMockServiceHostChanger(object : ServiceHostChanger {
      override fun getResultUrl(): String = url
    }) {
      testAction(EduAIServiceChangeHost.ACTION_ID)
    }
  }

  companion object {
    @NonNls
    private const val LOCALHOST_URL = "http://localhost:666/"
  }
}