package com.jetbrains.edu.integration.stepik

import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikUser
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.commons.codec.binary.Base64
import org.apache.http.Consts
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
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
import java.util.*
import java.util.regex.Pattern


abstract class StepikTestCase : EduTestCase() {
  companion object {
    private const val CSRF = "csrfmiddlewaretoken"
  }

  private lateinit var user: StepikUser
  private lateinit var httpClient: CloseableHttpClient

  override fun setUp() {
    super.setUp()
    val mockStepikConnector = StepikConnector.getInstance() as MockStepikConnector
    mockStepikConnector.baseUrl = StepikNames.STEPIK_URL
    httpClient = HttpClients.createDefault()
    login()
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
    EduSettings.getInstance().user = null
    super.tearDown()
  }

  private fun login() {
    val tokenInfo = getTokens()!!
    user = login(tokenInfo)
    EduSettings.getInstance().user = user
    println("Logged in as ${user.firstName} ${user.lastName}")
  }

  private fun login(tokenInfo: TokenInfo): StepikUser {
    val user = StepikUser(tokenInfo)

    val currentUser = StepikConnector.getInstance().getCurrentUserInfo(user)
    if (currentUser != null) {
      user.userInfo = currentUser
    }
    return user
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

  private fun getTokens(): TokenInfo? {
    val parameters = ArrayList<NameValuePair>(listOf(BasicNameValuePair("grant_type", "client_credentials")))

    val clientSecret = System.getenv("STEPIK_TEST_CLIENT_SECRET")
    if (clientSecret == null || clientSecret.isEmpty()) {
      LOG.error("Test client secret is not provided")
      return null
    }

    val clientId = System.getenv("STEPIK_TEST_CLIENT_ID")
    if (clientId == null || clientId.isEmpty()) {
      LOG.error("Test client id is not provided")
      return null
    }

    // If we can't get tokens, there are might a problem with basic auth that used on Stepik by default
    // and disabled for us as we use oauth2 in our tests.
    // Check that:
    // 1. Teamcity didn't change ips of agent
    // 2. Stepik still have our server in whitelist
    return getTokens(parameters, "$clientId:$clientSecret")
  }

  private fun getTokens(parameters: List<NameValuePair>, credentials: String): TokenInfo? {
    val request = HttpPost(StepikNames.TOKEN_URL)
    request.addHeader("Authorization", "Basic " + Base64.encodeBase64String(credentials.toByteArray(Consts.UTF_8)))
    request.entity = UrlEncodedFormEntity(parameters, Consts.UTF_8)

    val response = httpClient.execute(request)
    val statusLine = response.statusLine
    val responseEntity = response.entity
    val responseString = if (responseEntity != null) EntityUtils.toString(responseEntity) else ""
    EntityUtils.consume(responseEntity)
    if (statusLine.statusCode == HttpStatus.SC_OK) {
      return StepikConnector.getInstance().objectMapper.readValue(responseString, TokenInfo::class.java)
    }
    else {
      LOG.warn("Failed to get tokens: " + statusLine.statusCode + statusLine.reasonPhrase)
    }

    return null
  }
}