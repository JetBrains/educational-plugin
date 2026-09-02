package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.CourseCertificateIssueResponse
import com.jetbrains.edu.learning.marketplace.api.CourseCertificationSupportResponse
import com.jetbrains.edu.learning.marketplace.api.LearningCenterConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CourseCertificateManagerTest : EduTestCase() {

  private lateinit var learningCenterConnector: LearningCenterConnector
  private lateinit var marketplaceConnector: MarketplaceConnector

  private val manager: CourseCertificateManager
    get() = CourseCertificateManager.getInstance(project)

  override fun setUp() {
    super.setUp()
    learningCenterConnector = mockService<LearningCenterConnector>(application)
    coEvery { learningCenterConnector.courseCertificationSupport(any()) } throws
      AssertionError("Unexpected `courseCertificationSupport` call")
    coEvery { learningCenterConnector.tryIssueCertificate(any(), any()) } throws
      AssertionError("Unexpected `tryIssueCertificate` call")

    marketplaceConnector = mockService<MarketplaceConnector>(application)

    val submissionsManager = mockService<SubmissionsManager>(project)
    justRun { submissionsManager.prepareSubmissionsContentWhenLoggedIn(any()) }

    every { marketplaceConnector.isLoggedIn() } returns false
  }

  @Test
  fun `test certification support is requested for not logged in user`() = runTest {
    createMarketplaceCourse()
    coEvery { learningCenterConnector.courseCertificationSupport(COURSE_ID) } returns
      Ok(CourseCertificationSupportResponse.Supported(50))

    val state = manager.updateCertificateState()

    assertEquals(CourseCertificationState.Supported(50), state)
    coVerify(exactly = 0) { learningCenterConnector.tryIssueCertificate(any(), any()) }
  }

  @Test
  fun `test course without certification`() = runTest {
    createMarketplaceCourse()
    coEvery { learningCenterConnector.courseCertificationSupport(COURSE_ID) } returns
      Ok(CourseCertificationSupportResponse.Unsupported)

    assertFalse(manager.hasCertification())

    assertEquals(CourseCertificationState.Unsupported, manager.state)
    assertFalse(manager.hasCertification())
  }

  @Test
  fun `test certification state is not requested for non marketplace course`() = runTest {
    courseWithFiles(language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }

    assertFalse(manager.hasCertification())

    assertEquals(CourseCertificationState.Unsupported, manager.state)
    coVerify(exactly = 0) { learningCenterConnector.courseCertificationSupport(any()) }
  }

  @Test
  fun `test required progress is stored and certificate is issued when it is reached`() = runTest {
    every { marketplaceConnector.isLoggedIn() } returns true
    val course = createMarketplaceCourse()
    coEvery { learningCenterConnector.tryIssueCertificate(0, COURSE_ID) } returns Ok(CourseCertificateIssueResponse.NotReady(50))
    coEvery { learningCenterConnector.tryIssueCertificate(50, COURSE_ID) } returns
      Ok(CourseCertificateIssueResponse.CertificateReady(CERTIFICATE_ID, true))

    val state = manager.updateCertificateState()

    assertEquals(CourseCertificationState.Supported(50), state)
    assertTrue(manager.hasCertification())

    // A half of the course is completed, so the required progress is reached
    course.solveTask(0)
    manager.updateCertificateState()

    assertEquals(CourseCertificationState.Issued(CERTIFICATE_ID, true), manager.state)
  }

  @Test
  fun `test certificate already requested by client is issued`() = runTest {
    every { marketplaceConnector.isLoggedIn() } returns true
    createMarketplaceCourse(solvedTasks = 2)
    coEvery { learningCenterConnector.tryIssueCertificate(100, COURSE_ID) } returns
      Ok(CourseCertificateIssueResponse.CertificateReady(CERTIFICATE_ID, false))

    val state = manager.updateCertificateState()
    assertEquals(CourseCertificationState.Issued(CERTIFICATE_ID, false), state)
    // Other achievement dialogs still shouldn't be shown for such a course
    assertTrue(manager.hasCertification())
  }

  @Test
  fun `test certificate is issued only once`() = runTest {
    every { marketplaceConnector.isLoggedIn() } returns true
    createMarketplaceCourse(solvedTasks = 2)
    var isFirst = true
    coEvery { learningCenterConnector.tryIssueCertificate(100, COURSE_ID) } answers {
      Ok(CourseCertificateIssueResponse.CertificateReady(CERTIFICATE_ID, isFirst)).also { isFirst = false }
    }

    repeat(3) {
      manager.updateCertificateState()
    }

    // we check for isFirstClientRequest twice
    coVerify(exactly = 2) { learningCenterConnector.tryIssueCertificate(any(), any()) }
  }

  @Test
  fun `test course with unsupported certificate issuing`() = runTest {
    every { marketplaceConnector.isLoggedIn() } returns true
    createMarketplaceCourse()
    coEvery { learningCenterConnector.courseCertificationSupport(COURSE_ID) } returns Ok(CourseCertificationSupportResponse.Unsupported)

    assertFalse(manager.hasCertification())
    assertEquals(CourseCertificationState.Unsupported, manager.state)
  }

  @Test
  fun `test failed request doesn't change the state`() = runTest {
    createMarketplaceCourse()
    coEvery { learningCenterConnector.courseCertificationSupport(COURSE_ID) } returns Err("Something went wrong")

    val state = manager.updateCertificateState()

    assertEquals(CourseCertificationState.Unknown, state)
    coVerify(exactly = 1) { learningCenterConnector.courseCertificationSupport(COURSE_ID) }
  }

  @Test
  fun `test overridden completed percent is reported`() = runTest {
    every { marketplaceConnector.isLoggedIn() } returns true
    // No task is solved, so the actual progress is 0
    createMarketplaceCourse()
    CourseCertificatePercentageService.getInstance(project).completedPercent = 80
    coEvery { learningCenterConnector.tryIssueCertificate(80, COURSE_ID) } returns
      Ok(CourseCertificateIssueResponse.CertificateReady(CERTIFICATE_ID, true))

    val state = manager.updateCertificateState()

    assertEquals(CourseCertificationState.Issued(CERTIFICATE_ID, true), state)
  }

  private fun createMarketplaceCourse(solvedTasks: Int = 0): EduCourse {
    val course = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse, id = COURSE_ID) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
        eduTask("task2", stepId = 2) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 1
    } as EduCourse

    repeat(solvedTasks) { course.solveTask(it) }
    return course
  }

  private fun EduCourse.solveTask(index: Int) {
    lessons.first().taskList[index].status = CheckStatus.Solved
  }

  companion object {
    private const val COURSE_ID = 1
    private const val CERTIFICATE_ID = "certificate-id"
  }
}
