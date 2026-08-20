package com.jetbrains.edu.learning.marketplace

import com.intellij.util.application
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceLoginListener
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.SubmissionsTestBase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.verify
import org.junit.Test

class MarketplaceLoginListenerTest : SubmissionsTestBase() {

  @Test
  fun `test login prepares submissions content and uploads local submissions`() {
    val submissionsManager = mockService<SubmissionsManager>(project)
    justRun { submissionsManager.prepareSubmissionsContentWhenLoggedIn(any()) }
    val connector = mockService<MarketplaceSubmissionsConnector>(application)
    coJustRun { connector.uploadLocalSubmissions(any(), any()) }
    val course = createEduCourse()

    application.messageBus.syncPublisher(MarketplaceLoginListener.LOGIN_TOPIC).onLoginSuccess()

    verify(exactly = 1) { submissionsManager.prepareSubmissionsContentWhenLoggedIn(any()) }
    coVerify(exactly = 1) { connector.uploadLocalSubmissions(project, course) }
  }

  private fun createEduCourse(): EduCourse = courseWithFiles(
    language = FakeGradleBasedLanguage,
    courseProducer = ::EduCourse,
    id = 1
  ) {
    lesson("lesson1") {
      eduTask("task1", stepId = 1) {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("test/Tests1.kt", "fun tests1() {}")
      }
      eduTask("task2", stepId = 2) {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("test/Tests2.kt", "fun tests2() {}")
      }
    }
  }.apply {
    isMarketplace = true
    marketplaceCourseVersion = 1
  } as EduCourse
}
