package com.jetbrains.edu.learning.services.action

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.services.dialog.ServiceHostChanger
import com.jetbrains.edu.learning.services.dialog.withMockServiceHostChanger
import com.jetbrains.edu.learning.testAction
import org.junit.Test

abstract class ServiceChangeHostActionTest : EduTestCase() {
  protected abstract val actionId: String
  protected abstract val propertyName: String
  protected abstract var initialUrl: String

  protected abstract val productionUrl: String
  protected abstract val stagingUrl: String
  protected abstract val localhostUrl: String

  override fun setUp() {
    super.setUp()
    initialUrl = getSelectedUrl()
  }

  override fun tearDown() {
    try {
      PropertiesComponent.getInstance().setValue(propertyName, initialUrl, productionUrl)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  protected abstract fun getSelectedUrl(): String

  @Test
  fun `test change production to staging host`() = doTestServiceChange(productionUrl, stagingUrl)

  @Test
  fun `test change back from staging to production`() = doTestServiceChange(stagingUrl, productionUrl)

  @Test
  fun `test change production to localhost`() = doTestServiceChange(productionUrl, localhostUrl)

  @Test
  fun `test change back from localhost to production`() = doTestServiceChange(localhostUrl, productionUrl)

  @Test
  fun `test change staging to localhost`() = doTestServiceChange(stagingUrl, localhostUrl)

  @Test
  fun `test change back from localhost to staging`() = doTestServiceChange(localhostUrl, stagingUrl)

  private fun doTestServiceChange(initialHost: String, newHost: String) {
    PropertiesComponent.getInstance().setValue(propertyName, initialHost)
    assertEquals(initialHost, getSelectedUrl())
    doServiceChangeHostAction(newHost)
    assertEquals(newHost, getSelectedUrl())
  }

  private fun doServiceChangeHostAction(url: String) {
    withMockServiceHostChanger(object : ServiceHostChanger {
      override fun getResultUrl(): String = url
    }) {
      testAction(actionId)
    }
  }
}