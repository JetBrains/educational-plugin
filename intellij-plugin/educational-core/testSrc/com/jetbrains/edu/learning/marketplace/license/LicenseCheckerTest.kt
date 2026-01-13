package com.jetbrains.edu.learning.marketplace.license

import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.common.waitUntil
import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.marketplace.license.api.LicenseConnector
import com.jetbrains.edu.learning.marketplace.metadata.getRandomTrustedUrl
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings
import com.jetbrains.edu.learning.mockService
import io.mockk.coEvery
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

class LicenseCheckerTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    LicenseLinkSettings.getInstance(project).link = getRandomTrustedUrl()
  }

  @Test
  fun `test invoking scheduleLicenseCheck runs check and restarts timer`() = runTest {
    val invocationCounter = AtomicInteger(0)
    val licenseConnector = mockService<LicenseConnector>(application)
    coEvery { licenseConnector.checkLicense(any()) } coAnswers {
      invocationCounter.incrementAndGet()
      LicenseState.VALID
    }

    // init the license checker manually to use test coroutine scope instead of default
    // this allows using virtual test coroutines time and bypass delays in license check
    val licenseChecker = initLicenseChecker()
    // use already initialized service, because project.service() will reinit service with the default dispatcher
    licenseChecker.scheduleLicenseCheck()
    assertTrue(licenseChecker.isRunning)

    withTimeout(1.seconds) {
      waitUntil { invocationCounter.get() == 1 }
      licenseChecker.scheduleLicenseCheck()
      waitUntil { invocationCounter.get() == 2 }
    }
  }

  @Test
  fun `test license check is invoked again after timeout`() = runTest {
    val invocationCounter = AtomicInteger(0)
    val licenseConnector = mockService<LicenseConnector>(application)
    coEvery { licenseConnector.checkLicense(any()) } coAnswers {
      invocationCounter.incrementAndGet()
      LicenseState.VALID
    }

    val licenceChecker = initLicenseChecker()
    licenceChecker.scheduleLicenseCheck()

    withCustomCheckInterval(1) {
      withTimeout(2.seconds) {
        assertTrue(licenceChecker.isRunning)
        waitUntil { invocationCounter.get() == 1 }
        waitUntil { invocationCounter.get() == 2 }
      }
    }
  }

  private fun TestScope.initLicenseChecker(): LicenseChecker {
    val serviceScope = backgroundScope.childScope("LicenseChecker-scope")
    return LicenseChecker(project, serviceScope)
  }

  private suspend fun withCustomCheckInterval(interval: Int, action: suspend () -> Unit) {
    val registryValue = Registry.get(LicenseChecker.REGISTRY_KEY)
    val oldValue = registryValue.asInteger()
    registryValue.setValue(interval)
    try {
      action()
    }
    finally {
      registryValue.setValue(oldValue)
    }
  }
}