package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
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
import java.util.*
import java.util.regex.Pattern


abstract class StepikTestCase : EduTestCase() {
  companion object {
    private const val CLIENT_ID = "wHohrJv83oYoFYmgWYwEDW5ZNS1ntVRueWjMyQpm"
    private const val CSRF = "csrfmiddlewaretoken"
  }

  private lateinit var user: StepicUser

  private lateinit var httpClient: CloseableHttpClient

  override fun setUp() {
    super.setUp()

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
    user = StepikAuthorizedClient.login(tokenInfo)
    EduSettings.getInstance().user = user
    println("Logged in as ${user.firstName} ${user.lastName}")
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

  fun checkCourseUploaded(course: RemoteCourse) {
    val uploadedCourse = findUploadedCourse(course)
    assertNotNull("Uploaded courses not found among courses available to instructor", uploadedCourse)
    println("Course with id ${(uploadedCourse as RemoteCourse).id} was uploaded successfully")
  }

  protected fun findUploadedCourse(course: RemoteCourse): Course? {
    val courses = EduUtils.getCoursesUnderProgress()
    assertTrue(courses!!.size >= 1)
    return courses.find { c -> (c is RemoteCourse) && (c.id == course.id) && (c.name == course.name) }
  }

  private fun getTokens(): StepikWrappers.TokenInfo? {
    val parameters = ArrayList<NameValuePair>(listOf(BasicNameValuePair ("grant_type", "client_credentials")))
    val clientSecret = System.getenv("STEPIK_TEST_CLIENT_SECRET")
    if (clientSecret == null || clientSecret.isEmpty()) {
      LOG.error("Test client secret is not provided")
      return null
    }
    return StepikAuthorizedClient.getTokens(parameters, "$CLIENT_ID:$clientSecret")
  }
}