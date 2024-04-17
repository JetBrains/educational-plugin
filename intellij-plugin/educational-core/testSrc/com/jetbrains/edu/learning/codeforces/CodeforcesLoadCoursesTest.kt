package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.MockCodeforcesConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class CodeforcesLoadCoursesTest : CodeforcesTestCase() {

  private val mockConnector: MockCodeforcesConnector get() = CodeforcesConnector.getInstance() as MockCodeforcesConnector

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { _, _ ->
      MockResponseFactory.fromFile(File(testDataPath, "${getTestName(true).trim()}.html").path)
    }
  }

  @Test
  fun `test basic load courses`() {
    val coursesGroups = loadCourses()
    assertEquals(2, coursesGroups.size)
    val first = coursesGroups.first()
    assertEquals(EduCoreBundle.message("course.dialog.codeforces.upcoming"), first.name)
    assertEquals(4, first.courses.size)
    val second = coursesGroups[1]
    assertEquals(EduCoreBundle.message("course.dialog.codeforces.recent"), second.name)
    assertEquals(100, second.courses.size)
  }

  @Test
  fun `test upcoming contests only`() {
    val coursesGroups = loadCourses()
    assertEquals(1, coursesGroups.size)
    val first = coursesGroups.first()
    assertEquals(EduCoreBundle.message("course.dialog.codeforces.upcoming"), first.name)
    assertEquals(4, first.courses.size)
  }

  @Test
  fun `test past contests only`() {
    val coursesGroups = loadCourses()
    assertEquals(1, coursesGroups.size)
    val first = coursesGroups.first()
    assertEquals(EduCoreBundle.message("course.dialog.codeforces.recent"), first.name)
    assertEquals(100, first.courses.size)
  }

  private fun loadCourses() = runBlocking {
    CodeforcesPlatformProvider().loadCourses()
  }
}