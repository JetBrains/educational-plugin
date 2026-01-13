package com.jetbrains.edu.learning.marketplace.license

import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.common.waitUntil
import com.intellij.util.application
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.marketplace.license.api.LicenseConnector
import com.jetbrains.edu.learning.marketplace.metadata.getRandomTrustedUrl
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import io.mockk.coEvery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class LicenseCheckerTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  override fun setUp() {
    super.setUp()
    val licenseConnector = mockService<LicenseConnector>(application)
    coEvery { licenseConnector.checkLicense(any()) } returns Ok(true)
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
    createCourseStructure(course, metadata = mapOf("license_url" to getRandomTrustedUrl()))

    assertTrue(LicenseChecker.getInstance(project).isRunning)

    // switching to default dispatcher to use timeout with real time instead of virtual kotlinx.coroutines.test time
    withContext(Dispatchers.Default) {
      withTimeout(1.seconds) {
        waitUntil { LicenseChecker.getInstance(project).invocationNumber == 1 }
      }
    }
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
  }

  @Test
  fun `test invoking scheduleLicenseCheck runs check and restarts timer`() = runTest {
    val course = course {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    }

    createCourseStructure(course, metadata = mapOf("license_url" to getRandomTrustedUrl()))

    assertTrue(LicenseChecker.getInstance(project).isRunning)

    // switching to default dispatcher to use timeout with real time instead of virtual kotlinx.coroutines.test time
    withContext(Dispatchers.Default) {
      withTimeout(1.seconds) {
        waitUntil { LicenseChecker.getInstance(project).invocationNumber == 1 }
        LicenseChecker.getInstance(project).scheduleLicenseCheck()
        waitUntil { LicenseChecker.getInstance(project).invocationNumber == 2 }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `test license check is invoked again after timeout`() = runTest {
    val course = course {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    }

    withCustomCheckInterval(1) {
      createCourseStructure(course, metadata = mapOf("license_url" to getRandomTrustedUrl()))

      // switching to default dispatcher to use timeout with real time instead of virtual kotlinx.coroutines.test time
      withContext(Dispatchers.Default) {
        withTimeout(2.seconds) {
          assertTrue(LicenseChecker.getInstance(project).isRunning)
          waitUntil { LicenseChecker.getInstance(project).invocationNumber == 1 }
          waitUntil { LicenseChecker.getInstance(project).invocationNumber == 2 }
        }
      }
    }
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