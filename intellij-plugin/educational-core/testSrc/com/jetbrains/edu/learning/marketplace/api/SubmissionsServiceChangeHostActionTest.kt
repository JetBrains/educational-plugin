package com.jetbrains.edu.learning.marketplace.api

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceChangeHost
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.services.dialog.ServiceHostChanger
import com.jetbrains.edu.learning.services.dialog.withMockServiceHostChanger
import com.jetbrains.edu.learning.testAction
import org.jetbrains.annotations.NonNls
import org.junit.Test

class SubmissionsServiceChangeHostActionTest : EduTestCase() {
  private var initialUrl: String = SubmissionsServiceHost.PRODUCTION.url

  override fun setUp() {
    super.setUp()
    initialUrl = SubmissionsServiceHost.getSelectedUrl()
  }

  override fun tearDown() {
    try {
      PropertiesComponent.getInstance().setValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, initialUrl, SubmissionsServiceHost.PRODUCTION.url)
    } finally {
      super.tearDown()
    }
  }

  @Test
  fun `test change production to staging host`() =
    doTestSubmissionsServiceChanged(SubmissionsServiceHost.PRODUCTION.url, SubmissionsServiceHost.STAGING.url)

  @Test
  fun `test change back from staging to production`() =
    doTestSubmissionsServiceChanged(SubmissionsServiceHost.STAGING.url, SubmissionsServiceHost.PRODUCTION.url)

  @Test
  fun `test change production to localhost`() =
    doTestSubmissionsServiceChanged(SubmissionsServiceHost.PRODUCTION.url, LOCALHOST_URL)

  @Test
  fun `test change back from localhost to production`() =
    doTestSubmissionsServiceChanged(LOCALHOST_URL, SubmissionsServiceHost.PRODUCTION.url)

  @Test
  fun `test change staging to localhost`() = doTestSubmissionsServiceChanged(SubmissionsServiceHost.STAGING.url, LOCALHOST_URL)

  @Test
  fun `test change back from localhost to staging`() = doTestSubmissionsServiceChanged(LOCALHOST_URL, SubmissionsServiceHost.STAGING.url)

  private fun doTestSubmissionsServiceChanged(initialHost: String, newHost: String) {
    PropertiesComponent.getInstance().setValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, initialHost)
    assertEquals(initialHost, SubmissionsServiceHost.getSelectedUrl())
    doSubmissionsServiceChangeHostAction(newHost)
    assertEquals(newHost, SubmissionsServiceHost.getSelectedUrl())
  }

  private fun doSubmissionsServiceChangeHostAction(url: String) {
    withMockServiceHostChanger(object : ServiceHostChanger {
      override fun getResultUrl(): String = url
    }) {
      testAction(SubmissionsServiceChangeHost.ACTION_ID)
    }
  }

  companion object {
    @NonNls
    private const val LOCALHOST_URL = "http://localhost:666/"
  }
}