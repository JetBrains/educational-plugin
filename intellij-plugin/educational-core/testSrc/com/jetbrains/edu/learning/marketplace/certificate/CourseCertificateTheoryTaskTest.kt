package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.mockService
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.Test
import java.util.concurrent.CompletableFuture

/**
 * Checks that the user is notified about an earned certificate when a theory task of a marketplace course
 * becomes solved on opening it in an editor, see `EduEditorFactoryListener.markTheoryTaskCompleted`.
 */
class CourseCertificateTheoryTaskTest : EduTestCase() {

  private lateinit var manager: CourseCertificateManager

  override fun setUp() {
    super.setUp()
    manager = mockService<CourseCertificateManager>(project)
    every { manager.notifyIfEarned(any()) } just runs

    // a theory task of a marketplace course is posted as completed, and the real check goes to the network
    val marketplaceConnector = mockService<MarketplaceConnector>(application)
    every { marketplaceConnector.isLoggedInAsync() } returns CompletableFuture.completedFuture(false)
  }

  @Test
  fun `test user is notified when theory task is solved on opening`() {
    val course = createMarketplaceCourse()
    val task = course.findTask("lesson1", "theory")
    assertEquals(CheckStatus.Unchecked, task.status)
    var statusOnNotify: CheckStatus? = null
    every { manager.notifyIfEarned(any()) } answers { statusOnNotify = task.status }

    task.openTaskFileInEditor("Task.txt")

    assertEquals(CheckStatus.Solved, task.status)
    verify(exactly = 1) { manager.notifyIfEarned(course) }
    // the certificate progress is counted from solved tasks, so the task must already be solved by then
    assertEquals(CheckStatus.Solved, statusOnNotify)
  }

  @Test
  fun `test user is not notified when already solved theory task is opened`() {
    val course = createMarketplaceCourse()
    val task = course.findTask("lesson1", "theory")
    task.status = CheckStatus.Solved

    task.openTaskFileInEditor("Task.txt")

    verify(exactly = 0) { manager.notifyIfEarned(any()) }
  }

  private fun createMarketplaceCourse(): EduCourse = courseWithFiles(courseProducer = ::EduCourse, id = COURSE_ID) {
    lesson("lesson1") {
      theoryTask("theory") { taskFile("Task.txt") }
      eduTask("task1") { taskFile("Task.txt") }
    }
  }.apply {
    isMarketplace = true
    marketplaceCourseVersion = 1
  } as EduCourse

  companion object {
    private const val COURSE_ID = 1
  }
}
