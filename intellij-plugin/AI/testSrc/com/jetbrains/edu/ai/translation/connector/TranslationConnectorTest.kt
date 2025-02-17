package com.jetbrains.edu.ai.translation.connector

import com.intellij.util.application
import com.jetbrains.edu.ai.translation.TranslationError
import com.jetbrains.edu.ai.translation.connector.TranslationConnectorTest.ResponseError.*
import com.jetbrains.edu.ai.translation.service.TranslationService
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.mockService
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.CourseTranslationResponse
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.ProtocolException
import java.net.UnknownHostException
import kotlin.test.assertIs

class TranslationConnectorTest : EduTestCase() {
  private val translationLanguage = TranslationLanguage.RUSSIAN

  @Test
  fun `test successful response`() {
    val course = courseWithFiles(id = 1) {
      lesson("lesson1", id = 2) {
        eduTask("task1", stepId = 3)
        eduTask("task2", stepId = 4)
      }
    } as EduCourse
    val expectedTranslationResponse = CourseTranslationResponse(TranslationVersion(1), translationLanguage, mapOf(), mapOf())

    val mockedService = mockk<TranslationService>()
    coEvery {
      mockedService.getTranslatedCourse(
        course.id,
        course.marketplaceCourseVersion,
        translationLanguage.name
      )
    } returns Response.success(expectedTranslationResponse)

    val connector = mockService<TranslationServiceConnector>(application)
    every { connector["translationService"]() } returns mockedService

    val response = runBlocking {
      connector.getTranslatedCourse(course.id, course.marketplaceCourseVersion, translationLanguage)
    }

    assertIs<Ok<*>>(response)
    assertEquals(expectedTranslationResponse, response.value)

    coVerify(exactly = 1) { mockedService.getTranslatedCourse(course.id, course.marketplaceCourseVersion, translationLanguage.name) }
  }

  @Test
  fun `test connection error`() = testError(TranslationError.CONNECTION_ERROR, ResponseException(UnknownHostException()))

  @Test
  fun `test different connection error`() = testError(TranslationError.CONNECTION_ERROR, ResponseException(ProtocolException()))

  @Test
  fun `test translation not found`() = testError(TranslationError.NO_TRANSLATION, ResponseCodeError(404))

  @Test
  fun `test translation unavailable`() = testError(TranslationError.TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS, ResponseCodeError(451))

  @Test
  fun `test service unavailable`() = testError(TranslationError.SERVICE_UNAVAILABLE, ResponseCodeError(503))

  @Test
  fun `test unknown service error`() = testError(TranslationError.SERVICE_UNAVAILABLE, ResponseCodeError(400))

  private fun testError(expectedTranslationError: TranslationError, error: ResponseError) {
    val course = courseWithFiles(id = 1) {
      lesson("lesson1", id = 2) {
        eduTask("task1", stepId = 3)
        eduTask("task2", stepId = 4)
      }
    } as EduCourse
    val mockedService = mockk<TranslationService>()
    coEvery {
      mockedService.getTranslatedCourse(
        course.id,
        course.marketplaceCourseVersion,
        translationLanguage.name
      )
    } failsWith error

    val connector = mockService<TranslationServiceConnector>(application)
    every { connector["translationService"]() } returns mockedService

    val response = runBlocking {
      connector.getTranslatedCourse(course.id, course.marketplaceCourseVersion, translationLanguage)
    }

    assertIs<Err<*>>(response)
    assertEquals(expectedTranslationError, response.error)

    coVerify(exactly = 1) { mockedService.getTranslatedCourse(course.id, course.marketplaceCourseVersion, translationLanguage.name) }
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
