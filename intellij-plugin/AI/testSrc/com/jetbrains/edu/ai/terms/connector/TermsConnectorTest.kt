package com.jetbrains.edu.ai.terms.connector

import com.intellij.util.application
import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.error.CommonAIServiceError
import com.jetbrains.edu.ai.terms.TermsError
import com.jetbrains.edu.ai.terms.connector.TermsConnectorTest.ResponseError.ResponseCodeError
import com.jetbrains.edu.ai.terms.connector.TermsConnectorTest.ResponseError.ResponseException
import com.jetbrains.edu.ai.terms.service.TermsService
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.mockService
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.CourseTermsResponse
import com.jetbrains.educational.terms.format.domain.TermsVersion
import io.mockk.MockKAdditionalAnswerScope
import io.mockk.MockKStubScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.ProtocolException
import java.net.UnknownHostException
import kotlin.test.assertIs

class TermsConnectorTest : EduTestCase() {
  private val translationLanguage = TranslationLanguage.ENGLISH

  @Test
  fun `test successful response`() {
    val course = courseWithFiles(id = 1) {
      lesson("lesson1", id = 2) {
        eduTask("task1", stepId = 3)
        eduTask("task2", stepId = 4)
      }
    } as EduCourse
    val expectedTermsResponse = CourseTermsResponse(TermsVersion(1), translationLanguage, mapOf())

    val mockedService = mockk<TermsService>()
    coEvery {
      mockedService.getCourseTerms(
        course.id,
        course.marketplaceCourseVersion,
        translationLanguage.name
      )
    } returns Response.success(expectedTermsResponse)

    val connector = mockService<TermsServiceConnector>(application)
    every { connector["termsService"]() } returns mockedService

    val response = runBlocking {
      connector.getCourseTerms(course.id, course.marketplaceCourseVersion, translationLanguage)
    }

    assertIs<Ok<*>>(response)
    assertEquals(expectedTermsResponse, response.value)

    coVerify(exactly = 1) { mockedService.getCourseTerms(course.id, course.marketplaceCourseVersion, translationLanguage.name) }
  }

  @Test
  fun `test connection error`() = testError(CommonAIServiceError.CONNECTION_ERROR, ResponseException(UnknownHostException()))

  @Test
  fun `test different connection error`() = testError(CommonAIServiceError.CONNECTION_ERROR, ResponseException(ProtocolException()))

  @Test
  fun `test terms not found`() = testError(TermsError.NO_TERMS, ResponseCodeError(404))

  @Test
  fun `test terms unavailable`() = testError(TermsError.TERMS_UNAVAILABLE_FOR_LEGAL_REASON, ResponseCodeError(451))

  @Test
  fun `test service unavailable`() = testError(CommonAIServiceError.SERVICE_UNAVAILABLE, ResponseCodeError(503))

  @Test
  fun `test unknown service error`() = testError(CommonAIServiceError.SERVICE_UNAVAILABLE, ResponseCodeError(400))

  private fun testError(expectedError: AIServiceError, error: ResponseError) {
    val course = courseWithFiles(id = 1) {
      lesson("lesson1", id = 2) {
        eduTask("task1", stepId = 3)
        eduTask("task2", stepId = 4)
      }
    } as EduCourse
    val mockedService = mockk<TermsService>()
    coEvery {
      mockedService.getCourseTerms(
        course.id,
        course.marketplaceCourseVersion,
        translationLanguage.name
      )
    } failsWith error

    val connector = mockService<TermsServiceConnector>(application)
    every { connector["termsService"]() } returns mockedService

    val response = runBlocking {
      connector.getCourseTerms(course.id, course.marketplaceCourseVersion, translationLanguage)
    }

    assertIs<Err<*>>(response)
    assertEquals(expectedError, response.error)

    coVerify(exactly = 1) { mockedService.getCourseTerms(course.id, course.marketplaceCourseVersion, translationLanguage.name) }
  }

  private infix fun <T : Any, B> MockKStubScope<Response<T>, B>.failsWith(
    error: ResponseError
  ): MockKAdditionalAnswerScope<Response<T>, B> = when (error) {
    is ResponseCodeError -> returns(Response.error(error.errorCode, "".toResponseBody()))
    is ResponseException -> throws(error.exception)
  }

  private sealed class ResponseError {
    data class ResponseCodeError(val errorCode: Int) : ResponseError()
    data class ResponseException(val exception: IOException) : ResponseError()
  }
}
