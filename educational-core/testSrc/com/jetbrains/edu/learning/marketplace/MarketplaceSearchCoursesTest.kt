package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.UserInfo
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector
import okhttp3.mockwebserver.RecordedRequest

class MarketplaceSearchCoursesTest : EduTestCase() {

  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector

  private fun configureCoursesResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("courses.json")
    }
  }

  fun `test courses loaded`() {
    configureCoursesResponse()
    doTestCoursesLoaded()
  }

  fun `test python en course created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val pythonCourse = courses.first()
    doTest(pythonCourse, 1, "Introduction to Python", "Python", "English", "Introduction course to Python",
           2, 5.0)
    assertEquals("JetBrains s.r.o.", pythonCourse.organization)
    checkAuthorFullNames(listOf("JetBrains s.r.o."), pythonCourse.authorFullNames)
    checkAuthors(listOf("FirstName LastName"), pythonCourse.authors)
  }

  fun `test java ru course created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val javaCourse = courses[1]
    doTest(javaCourse, 2, "Introduction to Java", "JAVA", "Russian",
           "Introduction course to Java", 5, 5.0)
    assertNull(javaCourse.organization)
    val expectedAuthors = listOf("user1 LastName1", "user2 LastName2")
    checkAuthorFullNames(expectedAuthors, javaCourse.authorFullNames)
    checkAuthors(expectedAuthors, javaCourse.authors)
  }

  fun `test scala course with environment created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val scalaCourse = courses[2]
    doTest(scalaCourse, 3, "Scala course", "Scala", "English",
           "Introduction course to Scala", 5, 4.75, "sbt")
    assertNull(scalaCourse.organization)
    val expectedAuthors = listOf("FirstName LastName")
    checkAuthorFullNames(expectedAuthors, scalaCourse.authorFullNames)
    checkAuthors(expectedAuthors, scalaCourse.authors)
  }

  fun `test all courses loaded`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      when (request.getOffset()) {
        0 -> mockResponse("courses_10.json")
        else -> mockResponse("courses.json")
      }
    }

    doTestCoursesLoaded(13)
  }

  fun `test course found by id`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("course_by_id.json")
    }
    val courseId = 1
    val course = MarketplaceConnector.getInstance().searchCourse(courseId)
    checkNotNull(course)
    doTest(course, courseId, "Introduction to Python", "Python", "English", "Introduction course to Python",
           2, 5.0)
    assertNull(course.organization)
    checkAuthorFullNames(listOf("FirstName LastName"), course.authorFullNames)
    checkAuthors(listOf("FirstName LastName"), course.authors)
  }

  private fun doTest(course: EduCourse,
                     expectedId: Int,
                     expectedName: String,
                     expectedLanguage: String,
                     expectedHumanLanguage: String,
                     expectedDescription: String,
                     expectedLearnersCount: Int,
                     expectedReviewScore: Double,
                     expectedEnvironment: String = DEFAULT_ENVIRONMENT) {
    assertEquals(expectedId, course.id)
    assertEquals(expectedName, course.name)
    assertEquals(expectedEnvironment, course.environment)
    assertEquals(expectedLanguage, course.language)
    assertEquals(expectedHumanLanguage, course.humanLanguage)
    assertEquals(expectedDescription, course.description)
    assertEquals(expectedLearnersCount, course.learnersCount)
    assertEquals(expectedReviewScore, course.reviewScore)
    assertTrue(course.isMarketplace)
  }

  private fun checkAuthorFullNames(expected: List<String>, actual: MutableList<String>) {
    assertEquals(expected.size, actual.size)
    for (n in expected.indices) {
      assertEquals(expected[n], actual[n])
    }
  }

  private fun checkAuthors(expected: List<String>, actual: MutableList<UserInfo>) {
    assertEquals(expected.size, actual.size)
    for (n in expected.indices) {
      assertEquals(expected[n], actual[n].getFullName())
    }
  }

  private fun RecordedRequest.getOffset(): Int {
    return body.readUtf8().substringAfter("offset: ").substringBefore("\\n").toInt()
  }

  private fun doTestCoursesLoaded(coursesNumber: Int = 3): List<EduCourse> {
    val courses = MarketplaceConnector.getInstance().searchCourses()
    assertEquals(coursesNumber, courses.size)
    return courses
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/searchCourses/"

  companion object {
    private val COURSES_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
  }
}