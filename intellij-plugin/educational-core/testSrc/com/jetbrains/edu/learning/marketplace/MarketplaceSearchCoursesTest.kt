package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector
import org.junit.Test
import java.util.*

class MarketplaceSearchCoursesTest : EduTestCase() {

  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector

  private fun configureCoursesResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request, path ->
      COURSES_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      val requestBody = request.body.readUtf8()
      when  {
        requestBody.isPluginsRequest() -> mockResponse("courses.json")
        requestBody.isUpdatesRequest() -> mockResponse("updates.json")
        else -> null
      }
    }
  }

  @Test
  fun `test courses loaded`() {
    configureCoursesResponse()
    doTestCoursesLoaded()
  }

  @Test
  fun `test python en course created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val pythonCourse = courses.first()
    doTest(pythonCourse, 1, "Introduction to Python", "Python", "English", "Introduction course to Python",
           2, 5.0)
    assertEquals("JetBrains s.r.o.", pythonCourse.organization)
    checkAuthorFullNames(listOf("JetBrains s.r.o."), pythonCourse.authorFullNames)
    checkAuthors(listOf("FirstName LastName"), pythonCourse.authors)
    assertEquals(13, pythonCourse.formatVersion)
  }

  @Test
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
    assertEquals(14, javaCourse.formatVersion)
  }

  @Test
  fun `test scala course with environment created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val scalaCourse = courses[2]
    doTest(scalaCourse, 3, "Scala course", "Scala", "English",
           "Introduction course to Scala", 5, 4.75, expectedEnvironment = "sbt")
    assertNull(scalaCourse.organization)
    val expectedAuthors = listOf("FirstName LastName")
    checkAuthorFullNames(expectedAuthors, scalaCourse.authorFullNames)
    checkAuthors(expectedAuthors, scalaCourse.authors)
    assertEquals(13, scalaCourse.formatVersion)
  }

  @Test
  fun `test all courses loaded`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, path ->
      COURSES_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      val requestBody = request.body.readUtf8()
      when {
        requestBody.isPluginsRequest() && (requestBody.getOffset() == 0) -> mockResponse("courses_10.json")
        requestBody.isPluginsRequest() && (requestBody.getOffset() == 10) -> mockResponse("courses.json")
        requestBody.isUpdatesRequest() ->  mockResponse("updates_10.json")
        else -> null
      }
    }

    doTestCoursesLoaded(13)
  }

  @Test
  fun `test course found by id`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, path ->
      COURSES_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      val requestBody = request.body.readUtf8()
      when  {
        requestBody.isPluginsRequest() -> mockResponse("course_by_id.json")
        requestBody.isUpdatesRequest() -> mockResponse("updates.json")
        else -> null
      }
    }
    val courseId = 1
    val course = MarketplaceConnector.getInstance().searchCourse(courseId)
    checkNotNull(course)
    doTest(course, courseId, "Introduction to Python", "Python", "English", "Introduction course to Python",
           2, 5.0)
  }

  @Test
  fun `test private course found`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, path ->
      COURSES_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      val requestBody = request.body.readUtf8()
      when  {
        requestBody.isPluginsRequest() -> mockResponse("private_course.json")
        requestBody.isUpdatesRequest() -> mockResponse("updates.json")
        else -> null
      }
    }
    val courseId = 1
    val course = MarketplaceConnector.getInstance().searchCourse(courseId)
    checkNotNull(course)
    doTest(course, courseId, "Introduction to Python", "Python", "English", "Introduction course to Python",
           2, 5.0, expectedIsPrivate = true)
    assertNull(course.organization)
    checkAuthorFullNames(listOf("FirstName LastName"), course.authorFullNames)
    checkAuthors(listOf("FirstName LastName"), course.authors)
  }

  private fun doTest(course: EduCourse,
                     expectedId: Int,
                     expectedName: String,
                     expectedLanguageId: String,
                     expectedHumanLanguage: String,
                     expectedDescription: String,
                     expectedLearnersCount: Int,
                     expectedReviewScore: Double,
                     expectedUpdateDate: Date = Date(1619697473000),
                     expectedCreateDate: Date = Date(1623321716000),
                     expectedEnvironment: String = DEFAULT_ENVIRONMENT,
                     expectedIsPrivate: Boolean = false,
                     expectedCourseLink: String = "${PLUGINS_REPOSITORY_URL}courseLink$REVIEWS",
                     expectedLicense: String = "https://licenses/") {
    assertEquals(expectedId, course.id)
    assertEquals(expectedName, course.name)
    assertEquals(expectedEnvironment, course.environment)
    assertEquals(expectedLanguageId, course.languageId)
    assertEquals(expectedHumanLanguage, course.humanLanguage)
    assertEquals(expectedDescription, course.description)
    assertEquals(expectedLearnersCount, course.learnersCount)
    assertEquals(expectedReviewScore, course.reviewScore)
    assertEquals(expectedUpdateDate, course.updateDate)
    assertEquals(expectedCreateDate, course.createDate)
    assertEquals(expectedIsPrivate, course.isMarketplacePrivate)
    assertEquals(expectedCourseLink, course.feedbackLink)
    assertEquals(expectedLicense, course.license)
    assertTrue(course.isMarketplace)
  }

  private fun checkAuthorFullNames(expected: List<String>, actual: List<String>) {
    assertEquals(expected.size, actual.size)
    for (n in expected.indices) {
      assertEquals(expected[n], actual[n])
    }
  }

  private fun checkAuthors(expected: List<String>, actual: List<UserInfo>) {
    assertEquals(expected.size, actual.size)
    for (n in expected.indices) {
      assertEquals(expected[n], actual[n].getFullName())
    }
  }

  private fun String.getOffset(): Int {
    return substringAfter("offset: ", "10").substringBefore("\\n").toInt()
  }

  private fun String.isPluginsRequest(): Boolean {
    return contains("plugins")
  }

  private fun String.isUpdatesRequest(): Boolean {
    return contains("updates")
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