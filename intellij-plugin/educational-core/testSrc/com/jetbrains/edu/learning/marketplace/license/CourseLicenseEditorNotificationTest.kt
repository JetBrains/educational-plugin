package com.jetbrains.edu.learning.marketplace.license

import com.intellij.util.application
import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.marketplace.license.api.LicenseConnector
import com.jetbrains.edu.learning.marketplace.metadata.getRandomTrustedUrl
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.mockService
import io.mockk.coEvery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CourseLicenseEditorNotificationTest : NotificationsTestBase() {
  override fun setUp() {
    super.setUp()
    courseWithFiles(createYamlConfigs = true) {
      lesson("lesson") {
        eduTask("task") {
          taskFile("task.txt", "some task text")
        }
      }
    }
  }

  @Test
  fun `test no editor notification is shown if active license is present`() = runTest {
    LicenseLinkSettings.getInstance(project).link = getRandomTrustedUrl()
    val connector = mockService<LicenseConnector>(application)
    coEvery { connector.checkLicense(any()) } returns LicenseState.VALID
    checkLicenseAndWait()
    checkNoEditorNotification<CourseLicenseEditorNotificationProvider>(findFile("lesson/task/task.txt"))
  }

  @Test
  fun `test no editor notification is shown if course does not require license`() {
    // license check should not be scheduled
    assertFalse(LicenseChecker.getInstance(project).scheduleLicenseCheck())
    checkNoEditorNotification<CourseLicenseEditorNotificationProvider>(findFile("lesson/task/task.txt"))
  }

  @Test
  fun `test editor notification is shown if license is expired`() = runTest {
    LicenseLinkSettings.getInstance(project).link = getRandomTrustedUrl()
    val connector = mockService<LicenseConnector>(application)
    coEvery { connector.checkLicense(any()) } returns LicenseState.INVALID
    checkLicenseAndWait()
    checkEditorNotification<CourseLicenseEditorNotificationProvider>(
      findFile("lesson/task/task.txt"),
      EduCoreBundle.message("license.notification.invalid.text")
    )
  }

  @Test
  fun `test editor notification with error message is shown if license could not be obtained`() = runTest {
    LicenseLinkSettings.getInstance(project).link = getRandomTrustedUrl()
    val connector = mockService<LicenseConnector>(application)
    coEvery { connector.checkLicense(any()) } returns LicenseState.ERROR
    checkLicenseAndWait()
    checkEditorNotification<CourseLicenseEditorNotificationProvider>(
      findFile("lesson/task/task.txt"),
      EduCoreBundle.message("license.notification.error.text")
    )
  }

  private suspend fun checkLicenseAndWait() {
    val licenseChecker = LicenseChecker.getInstance(project)
    licenseChecker.scheduleLicenseCheck()
    // wait for licenseState to update with a non-null value
    licenseChecker.licenseState.first { it != null }
  }
}