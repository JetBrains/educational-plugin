package com.jetbrains.edu.learning.marketplace.license

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.application
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.marketplace.license.api.LicenseConnector
import com.jetbrains.edu.learning.marketplace.metadata.getRandomTrustedUrl
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.mockService
import io.mockk.coEvery
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
  fun `test no editor notification is shown if active license is present`() {
    LicenseLinkSettings.getInstance(project).link = getRandomTrustedUrl()
    val connector = mockService<LicenseConnector>(application)
    coEvery { connector.checkLicense(any()) } returns Ok(true)
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
  fun `test editor notification is shown if license is expired`() {
    LicenseLinkSettings.getInstance(project).link = getRandomTrustedUrl()
    val connector = mockService<LicenseConnector>(application)
    coEvery { connector.checkLicense(any()) } returns Ok(false)
    checkLicenseAndWait()
    checkEditorNotification<CourseLicenseEditorNotificationProvider>(
      findFile("lesson/task/task.txt"),
      EduCoreBundle.message("license.notification.invalid.text")
    )
  }

  @Test
  fun `test editor notification with error message is shown if license could not be obtained`() {
    LicenseLinkSettings.getInstance(project).link = getRandomTrustedUrl()
    val connector = mockService<LicenseConnector>(application)
    coEvery { connector.checkLicense(any()) } returns Err("message")
    checkLicenseAndWait()
    checkEditorNotification<CourseLicenseEditorNotificationProvider>(
      findFile("lesson/task/task.txt"),
      EduCoreBundle.message("license.notification.error.text")
    )
  }

  private fun checkLicenseAndWait() {
    val prevLicenseCheckInvocationNumber = LicenseChecker.getInstance(project).invocationNumber
    LicenseChecker.getInstance(project).scheduleLicenseCheck()
    PlatformTestUtil.waitWhileBusy { LicenseChecker.getInstance(project).invocationNumber <= prevLicenseCheckInvocationNumber }
  }
}