package com.jetbrains.edu.integration.stepik

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikTestUtils
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.http.Consts
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import java.util.regex.Pattern


abstract class StepikTestCase : EduTestCase() {
  companion object {
    private const val CSRF = "csrfmiddlewaretoken"
  }

  private lateinit var httpClient: CloseableHttpClient

  override fun setUp() {
    super.setUp()
    val mockStepikConnector = StepikConnector.getInstance() as MockStepikConnector
    mockStepikConnector.setBaseUrl(StepikNames.STEPIK_URL, testRootDisposable)
    val user = StepikTestUtils.login(testRootDisposable)
    httpClient = HttpClients.custom()
      .setDefaultHeaders(listOf(getAuthorizationHeader(user.accessToken)))
      .setDefaultCookieStore(BasicCookieStore()).build()
  }

  private fun getAuthorizationHeader(accessToken: String): BasicHeader {
    return BasicHeader("Authorization", "Bearer $accessToken")
  }

  override fun tearDown() {
    val course = StudyTaskManager.getInstance(project).course
    if (course is EduCourse && course.isRemote) {
      removeUploadedCourse(course.id, course.getLessons().map { it.id })
    }
    super.tearDown()
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
    postCourseDelete.entity = UrlEncodedFormEntity(listOf(BasicNameValuePair(
      CSRF, getCSRFToken(text))), Consts.UTF_8)
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
    val deleteRequest = HttpDelete(
      StepikNames.STEPIK_API_URL + "lessons/" + lessonId)
    deleteRequest.addHeader("Referer", "${StepikNames.STEPIK_URL}/edit-lesson/$lessonId/step/1")
    httpClient.execute(deleteRequest)
    println("Lesson $lessonId deleted")
  }

  fun checkCourseUploaded(course: EduCourse) {
    val uploadedCourse = StepikConnector.getInstance().getCourseInfo(course.id, true)
    assertNotNull("Uploaded course not found among courses available to instructor", uploadedCourse)
    println("Course with id ${(uploadedCourse as EduCourse).id} was uploaded successfully")
  }
}
