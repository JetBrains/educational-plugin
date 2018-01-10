package com.jetbrains.edu.learning.stepik

import com.intellij.lang.LanguageExtensionPoint
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikAuthorizedClient.getTokens
import com.jetbrains.edu.learning.stepik.StepikAuthorizedClient.login
import org.apache.http.Consts
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.junit.Test
import java.util.*
import java.util.regex.Pattern


class StepikIntegrationTest : CCTestCase() {
  companion object {
    private const val CLIENT_ID = "wHohrJv83oYoFYmgWYwEDW5ZNS1ntVRueWjMyQpm"
    private const val CSRF = "csrfmiddlewaretoken"
  }

  private lateinit var user: StepicUser

  private lateinit var httpClient: CloseableHttpClient

  override fun setUp() {
    super.setUp()
    //it's not allowed to open a course without a language
    registerPlainTextConfigurator()

    val course = StudyTaskManager.getInstance(project).course!!
    course.name = "Stepik Integration Test Course"
    course.language = PlainTextLanguage.INSTANCE.id

    login()

    httpClient = StepikClient.getBuilder()
      .setDefaultHeaders(listOf(StepikAuthorizedClient.getAuthorizationHeader(user.accessToken)))
      .setDefaultCookieStore(BasicCookieStore()).build()
  }

  override fun tearDown() {
    val course = StudyTaskManager.getInstance(project).course
    if (course is RemoteCourse) {
      removeUploadedCourse(course.id, course.getLessons(true).map { it -> it.id })
    }
    EduSettings.getInstance().user = null
    super.tearDown()
  }

  private fun login() {
    val tokenInfo = getTokens()!!
    user = login(tokenInfo)
    EduSettings.getInstance().user = user
    println("Logged in as ${user.firstName} ${user.lastName}")
  }

  private fun registerPlainTextConfigurator() {
    val extension = LanguageExtensionPoint<Annotator>()
    extension.language = PlainTextLanguage.INSTANCE.id
    extension.implementationClass = PlainTextConfigurator::class.java.name
    PlatformTestUtil.registerExtension(
      ExtensionPointName.create(EduConfigurator.EP_NAME), extension, myFixture.testRootDisposable)
  }

  @Test
  fun testUploadCourse() {
    CCStepikConnector.postCourseWithProgress(project, StudyTaskManager.getInstance(project).course!!)
    checkCourseUploaded(StudyTaskManager.getInstance(project).course as RemoteCourse)
  }

  /**
   * There is no Stepik REST API to delete courses. So course removal is performed by replicating browser procedure:
   * 1. Get form with course removal confirmation
   * 2. Post confirmation providing csrf token parsed manually
   */
  private fun removeUploadedCourse(courseId: Int, lessonIds: List<Int>) {
    println("Removing course with id $courseId...")
    val deleteLink = "${StepikNames.STEPIK_URL}/course/$courseId/delete/"
    val getCourseDelete = HttpGet(deleteLink)
    val text = EntityUtils.toString(httpClient.execute(getCourseDelete)?.entity)
    val postCourseDelete = HttpPost(deleteLink)
    postCourseDelete.addHeader("Referer", deleteLink)
    postCourseDelete.entity = UrlEncodedFormEntity(listOf(BasicNameValuePair(CSRF, getCSRFToken(text))), Consts.UTF_8)
    val postResult = httpClient.execute(postCourseDelete)
    assertEquals("Failed to remove course with id $courseId", 302, postResult?.statusLine?.statusCode)
    for (lessonId in lessonIds) {
      deleteLesson(lessonId)
    }
    println("Course with id $courseId was successfully removed")
  }

  private fun getCSRFToken(text: String?): String {
    val pattern = Pattern.compile("name='$CSRF' value='([0-9a-zA-Z]+)'")
    val matcher = pattern.matcher(text)
    matcher.find()
    return matcher.group(1)
  }

  private fun deleteLesson(lessonId: Int) {
    val deleteRequest = HttpDelete(StepikNames.STEPIK_API_URL + StepikNames.LESSONS + lessonId)
    deleteRequest.addHeader("Referer", "${StepikNames.STEPIK_URL}/edit-lesson/$lessonId/step/1")
    httpClient.execute(deleteRequest)
    println("Lesson $lessonId deleted")
  }

  private fun checkCourseUploaded(course: RemoteCourse) {
    val courses = EduUtils.getCoursesUnderProgress()
    assertTrue(courses!!.size >= 1)
    val uploadedCourse = courses.find { c -> (c is RemoteCourse) && (c.id == course.id) && (c.name == course.name) }
    assertNotNull("Uploaded courses not found among courses available to instructor", uploadedCourse)
    println("Course with id ${(uploadedCourse as RemoteCourse).id} was uploaded successfully")
  }

  class PlainTextConfigurator : EduConfigurator<Unit> {
    override fun getCourseBuilder() = object : EduCourseBuilder<Unit> {
      override fun createTaskContent(project: Project, task: Task, parentDirectory: VirtualFile, course: Course): VirtualFile? = null
      override fun getLanguageSettings(): EduCourseBuilder.LanguageSettings<Unit> = EduCourseBuilder.LanguageSettings { Unit }
    }

    override fun getTestFileName() = "test.txt"

    override fun excludeFromArchive(name: String) = false

    override fun getTaskCheckerProvider() = TaskCheckerProvider { task, project -> TaskChecker(task, project) }
  }

  private fun getTokens(): StepikWrappers.TokenInfo? {
    val parameters = ArrayList<NameValuePair>(listOf(BasicNameValuePair ("grant_type", "client_credentials")))
    val clientSecret = System.getenv("STEPIK_TEST_CLIENT_SECRET")
    if (clientSecret == null || clientSecret.isEmpty()) {
      LOG.error("Test client secret is not provided")
      return null
    }
    return getTokens(parameters, "$CLIENT_ID:$clientSecret")
  }
}