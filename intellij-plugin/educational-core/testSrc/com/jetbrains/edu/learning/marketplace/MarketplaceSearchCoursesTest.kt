package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.courseFormat.Vendor
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
    doTest(
      course = pythonCourse,
      expectedId = 1,
      expectedName = "Introduction to Python",
      expectedLanguageId = "Python",
      expectedHumanLanguage = "English",
      expectedDescription = "Introduction course to Python",
      expectedLearnersCount = 2,
      expectedReviewScore = 5.0,
      expectedAuthorFullNames = listOf("JetBrains s.r.o."),
      expectedVendor = Vendor("JetBrains s.r.o.", url = "https://plugins.jetbrains.com/vendor/JetBrains")
    )
    checkAuthors(listOf("FirstName LastName"), pythonCourse.authors)
    assertEquals(13, pythonCourse.formatVersion)
  }

  @Test
  fun `test java ru course created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val javaCourse = courses[1]
    doTest(
      course = javaCourse,
      expectedId = 2,
      expectedName = "Introduction to Java",
      expectedLanguageId = "JAVA",
      expectedHumanLanguage = "Russian",
      expectedDescription = "Introduction course to Java",
      expectedLearnersCount = 5,
      expectedReviewScore = 5.0,
      expectedAuthorFullNames = listOf("user1 LastName1", "user2 LastName2")
    )
    checkAuthors(listOf("user1 LastName1", "user2 LastName2"), javaCourse.authors)
    assertEquals(14, javaCourse.formatVersion)
  }

  @Test
  fun `test scala course with environment created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val scalaCourse = courses[2]
    doTest(
      course = scalaCourse,
      expectedId = 3,
      expectedName = "Scala course",
      expectedLanguageId = "Scala",
      expectedHumanLanguage = "English",
      expectedDescription = "Introduction course to Scala",
      expectedLearnersCount = 5,
      expectedReviewScore = 4.75,
      expectedEnvironment = "sbt",
      expectedAuthorFullNames = listOf("FirstName LastName")
    )
    checkAuthors(listOf("FirstName LastName"), scalaCourse.authors)
    assertEquals(13, scalaCourse.formatVersion)
  }

  @Test
  fun `test regular user vendor course created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val kotlinCourse = courses[3]
    doTest(
      course = kotlinCourse,
      expectedId = 4,
      expectedName = "Kotlin course",
      expectedLanguageId = "kotlin",
      expectedHumanLanguage = "English",
      expectedDescription = "Introduction course to Kotlin",
      expectedLearnersCount = 3,
      expectedReviewScore = 4.5,
      expectedAuthorFullNames = listOf("Regular User"),
      expectedVendor = Vendor("Regular User", url = "https://plugins.jetbrains.com/vendor/12345678-1234-1234-1234-123456789abc")
    )
    checkAuthors(listOf("FirstName LastName"), kotlinCourse.authors)
    assertEquals(13, kotlinCourse.formatVersion)
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

    doTestCoursesLoaded(14)
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
    doTest(
      course = course,
      expectedId = courseId,
      expectedName = "Introduction to Python",
      expectedLanguageId = "Python",
      expectedHumanLanguage = "English",
      expectedDescription = "Introduction course to Python",
      expectedLearnersCount = 2,
      expectedReviewScore = 5.0,
      expectedAuthorFullNames = listOf("FirstName LastName")
    )
  }

  @Test
  fun `test private course found`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, path ->
      COURSES_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      val requestBody = request.body.readUtf8()
      when {
        requestBody.isPluginsRequest() -> mockResponse("private_course.json")
        requestBody.isUpdatesRequest() -> mockResponse("updates.json")
        else -> null
      }
    }
    val courseId = 1
    val course = MarketplaceConnector.getInstance().searchCourse(courseId)
    checkNotNull(course)
    doTest(
      course = course,
      expectedId = courseId,
      expectedName = "Introduction to Python",
      expectedLanguageId = "Python",
      expectedHumanLanguage = "English",
      expectedDescription = "Introduction course to Python",
      expectedLearnersCount = 2,
      expectedReviewScore = 5.0,
      expectedIsPrivate = true,
      expectedAuthorFullNames = listOf("FirstName LastName")
    )
    checkAuthors(listOf("FirstName LastName"), course.authors)
  }

  private fun doTest(
    course: EduCourse,
    expectedId: Int,
    expectedName: String,
    expectedLanguageId: String,
    expectedHumanLanguage: String,
    expectedDescription: String,
    expectedLearnersCount: Int,
    expectedReviewScore: Double,
    expectedAuthorFullNames: List<String>,
    expectedUpdateDate: Date = Date(1619697473000),
    expectedCreateDate: Date = Date(1623321716000),
    expectedEnvironment: String = DEFAULT_ENVIRONMENT,
    expectedIsPrivate: Boolean = false,
    expectedCourseLink: String = "${PLUGINS_REPOSITORY_URL}courseLink$REVIEWS",
    expectedLicense: String = "https://licenses/",
    expectedVendor: Vendor? = null
  ) {
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
    assertEquals(expectedVendor, course.vendor)
    assertEquals(expectedAuthorFullNames, course.authorFullNames)
    assertTrue(course.isMarketplace)
  }

  private fun checkAuthors(expected: List<String>, actual: List<UserInfo>) {
    assertEquals(expected.size, actual.size)
    for ((expectedFullName, actualUserInfo) in expected.zip(actual)) {
      assertEquals(expectedFullName, actualUserInfo.getFullName())
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

  private fun doTestCoursesLoaded(coursesNumber: Int = 4): List<EduCourse> {
    val courses = MarketplaceConnector.getInstance().searchCourses()
    assertEquals(coursesNumber, courses.size)
    return courses
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/searchCourses/"

  companion object {
    private val COURSES_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
  }
}
