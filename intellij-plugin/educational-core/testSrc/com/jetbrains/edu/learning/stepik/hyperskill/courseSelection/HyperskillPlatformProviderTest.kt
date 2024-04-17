package com.jetbrains.edu.learning.stepik.hyperskill.courseSelection

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.newproject.HyperskillCourseAdvertiser
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.hyperskill.api.*
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.defaultHyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.HyperskillPlatformProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HyperskillPlatformProviderTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logOutFakeHyperskillUser()
    CoursesStorage.getInstance().state.courses.clear()
  }

  @Test
  fun `test advertising course added when no JBA login`() {
    val courseGroups = loadCourses()

    assertTrue(courseGroups.size == 1)
    assertTrue(courseGroups[0].courses.size == 1)

    val course = courseGroups[0].courses.first()
    assertTrue(course is HyperskillCourseAdvertiser)
  }

  @Test
  fun `test selected project added`() {
    logInFakeHyperskillUser()
    val profile = HyperskillUserInfo().apply { hyperskillProjectId = 1 }
    val hyperskillCourse = defaultHyperskillCourse()

    mockConnector.apply {
      withResponseHandler(testRootDisposable) { request, _ ->
        MockResponseFactory.fromString(
          when (request.pathWithoutPrams) {
            "/api/profiles/current" -> objectMapper.writeValueAsString(ProfilesList().apply { profiles = listOf(profile) })
            "/api/projects/1" -> objectMapper.writeValueAsString(ProjectsList().also {
              it.projects = listOf(hyperskillCourse.hyperskillProject!!)
            })
            else -> return@withResponseHandler null
          }
        )
      }
    }

    val courseGroup = loadCourses().first()

    assertTrue(courseGroup.courses.size == 1)

    val course = courseGroup.courses.first()
    assertTrue(course is HyperskillCourse)
    assertEquals(1, course.id)
  }

  @Test
  fun `test local content added`() {
    val localHyperskillCourse = defaultHyperskillCourse()
    CoursesStorage.getInstance().addCourse(localHyperskillCourse, "", 1, 4)

    val courseGroup = loadCourses().first()
    assertEquals(1, courseGroup.courses.size)
    val courseFromProvider = courseGroup.courses.first()
    assertEquals(localHyperskillCourse.id, courseFromProvider.id)

    CoursesStorage.getInstance().removeCourseByLocation("")
  }

  private fun loadCourses() = runBlocking {
    HyperskillPlatformProvider().loadCourses()
  }
}