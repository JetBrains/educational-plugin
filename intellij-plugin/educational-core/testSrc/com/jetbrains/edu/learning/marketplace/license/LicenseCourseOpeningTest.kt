package com.jetbrains.edu.learning.marketplace.license

import com.intellij.util.application
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.marketplace.license.api.LicenseConnector
import com.jetbrains.edu.learning.marketplace.metadata.LicenseLinkMetadataProcessor
import com.jetbrains.edu.learning.marketplace.metadata.getRandomTrustedUrl
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LicenseCourseOpeningTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  private lateinit var licenseConnector: LicenseConnector

  override fun setUp() {
    super.setUp()
    licenseConnector = mockService<LicenseConnector>(application)
    coEvery { licenseConnector.checkLicense(any()) } returns LicenseState.VALID
  }

  @Test
  fun `test license check is scheduled on project opening for course with license url`() = runTest {
    val course = course {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    }
    createCourseStructure(course, metadata = mapOf(LicenseLinkMetadataProcessor.LICENSE_URL_PARAMETER_NAME to getRandomTrustedUrl()))
    assertTrue(LicenseChecker.getInstance(project).isRunning)
    LicenseChecker.getInstance(project).licenseState.first { it != null }
    coVerify(exactly = 1) { licenseConnector.checkLicense(any()) }
  }

  @Test
  fun `test license check is not scheduled on project opening for course without license url`() {
    val course = course {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    }
    createCourseStructure(course)
    assertFalse(LicenseChecker.getInstance(project).isRunning)
    coVerify(exactly = 0) { licenseConnector.checkLicense(any()) }
  }
}