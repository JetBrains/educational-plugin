package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.*
import com.jetbrains.edu.learning.mockService
import io.mockk.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class CourseCertificateCheckListenerTest : EduTestCase() {

  private lateinit var learningCenterConnector: LearningCenterConnector
  private lateinit var marketplaceConnector: MarketplaceConnector
  private lateinit var dialogUIFactory: CourseCertificateDialogUIFactory
  private lateinit var certificateDialog: CourseCertificateDialogUI

  private val dialogShown = AtomicBoolean()

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

    dialogUIFactory = mockService<CourseCertificateDialogUIFactory>(project)
    certificateDialog = mockk(relaxed = true)
    every { certificateDialog.show() } answers { dialogShown.set(true) }
    every { dialogUIFactory.create(any(), any()) } returns certificateDialog

    logIn(false)
  }

  @Test
  fun `test dialog is shown for issued certificate`() {
    logIn(true)
    val course = createMarketplaceCourse(solvedTasks = 2)
    coEvery { learningCenterConnector.tryIssueCertificate(100, COURSE_ID) } returns
      Ok(CourseCertificateIssueResponse.CertificatePending(CERTIFICATE_ID, true))

    checkTask(CheckStatus.Solved)
    waitForDialog()

    assertEquals(CourseCertificationState.Issued(CERTIFICATE_ID, true), manager.state)
    verify(exactly = 1) { dialogUIFactory.create(course, CERTIFICATE_ID) }
    verify(exactly = 1) { certificateDialog.show() }
  }

  @Test
  fun `test dialog is not shown for certificate already requested by client`() {
    logIn(true)
    createMarketplaceCourse(solvedTasks = 2)
    coEvery { learningCenterConnector.tryIssueCertificate(100, COURSE_ID) } returns
      Ok(CourseCertificateIssueResponse.CertificateReady(CERTIFICATE_ID, false))

    checkTask(CheckStatus.Solved)
    waitForCertificationUpdate()

    assertEquals(CourseCertificationState.Issued(CERTIFICATE_ID, false), manager.state)
    verify(exactly = 0) { certificateDialog.show() }
  }

  @Test
  fun `test dialog is shown without certificate id for not logged in user`() {
    val course = createMarketplaceCourse(solvedTasks = 2)
    coEvery { learningCenterConnector.courseCertificationSupport(COURSE_ID) } returns
      Ok(CourseCertificationSupportResponse.Supported(50))

    checkTask(CheckStatus.Solved)
    waitForDialog()

    assertEquals(CourseCertificationState.Supported(50), manager.state)
    verify(exactly = 1) { dialogUIFactory.create(course, null) }
    verify(exactly = 1) { certificateDialog.show() }
  }

  @Test
  fun `test dialog is not shown after it was dismissed for non logged in user`() {
    createMarketplaceCourse(solvedTasks = 2)
    coEvery { learningCenterConnector.courseCertificationSupport(COURSE_ID) } returns
      Ok(CourseCertificationSupportResponse.Supported(50))
    dialogUIFactory.dismiss()

    checkTask(CheckStatus.Solved)
    waitForCertificationUpdate()

    assertEquals(CourseCertificationState.Supported(50), manager.state)
    verify(exactly = 0) { certificateDialog.show() }
  }

  @Test
  fun `test dialog is shown after it was dismissed for logged in user`() {
    logIn(true)
    val course = createMarketplaceCourse(solvedTasks = 2)
    coEvery { learningCenterConnector.tryIssueCertificate(100, COURSE_ID) } returns
      Ok(CourseCertificateIssueResponse.CertificatePending(CERTIFICATE_ID, true))
    dialogUIFactory.dismiss()

    checkTask(CheckStatus.Solved)
    waitForDialog()

    assertEquals(CourseCertificationState.Issued(CERTIFICATE_ID, true), manager.state)
    verify(exactly = 1) { dialogUIFactory.create(course, CERTIFICATE_ID) }
    verify(exactly = 1) { certificateDialog.show() }
  }

  @Test
  fun `test certification state is not requested for failed task`() {
    logIn(true)
    createMarketplaceCourse(solvedTasks = 2)

    checkTask(CheckStatus.Failed)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

    assertEquals(CourseCertificationState.Unknown, manager.state)
    coVerify(exactly = 0) { learningCenterConnector.tryIssueCertificate(any(), any()) }
    verify(exactly = 0) { certificateDialog.show() }
  }

  private fun checkTask(status: CheckStatus, task: Task = findTask(0, 0)) {
    CourseCertificateCheckListener().afterCheck(project, task, CheckResult(status))
  }

  private fun waitForDialog() {
    PlatformTestUtil.waitWhileBusy { !dialogShown.get() }
  }

  /**
   * The state is updated before the dialog is shown, so the event queue has to be drained afterward
   * to be sure the dialog is not shown
   */
  private fun waitForCertificationUpdate() {
    PlatformTestUtil.waitWhileBusy { manager.state == CourseCertificationState.Unknown }
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }

  private fun logIn(loggedIn: Boolean) {
    every { marketplaceConnector.isLoggedIn() } returns loggedIn
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

    repeat(solvedTasks) { course.lessons.first().taskList[it].status = CheckStatus.Solved }
    return course
  }

  companion object {
    private const val COURSE_ID = 1
    private const val CERTIFICATE_ID = "certificate-id"
  }
}
