package com.jetbrains.edu.ai.translation

import com.intellij.util.application
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.waitFor
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.CourseTranslationResponse
import com.jetbrains.educational.translation.format.TranslatedText
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class TranslationTest : EduTestCase() {

  override fun tearDown() {
    try {
      // TODO: improve `LightTestServiceStateHelper` to support services from any module
      TranslationProjectSettings.getInstance(project).cleanUpState()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test translation loading`() {
    // given
    val course = courseWithFiles(id = 1) {
      lesson("lesson1", id = 2) {
        eduTask("task1", stepId = 3)
        eduTask("task2", stepId = 4)
      }
    } as EduCourse

    val translationLanguage = TranslationLanguage.RUSSIAN
    val translationVersion = 1

    val response = CourseTranslationResponse(
      TranslationVersion(translationVersion),
      translationLanguage,
      mapOf(
        "3" to TranslatedText(translationLanguage, "description 1 translated"),
        "4" to TranslatedText(translationLanguage, "description 2 translated")
      ),
      mapOf(
        "2" to "lesson1 translated",
        "3" to "task1 translated",
        "4" to "task2 translated",
      )
    )

    val mockedService = mockService<TranslationServiceConnector>(application)
    coEvery { mockedService.getTranslatedCourse(course.id, course.marketplaceCourseVersion, translationLanguage) } returns Ok(response)

    // when
    performAndWait(expectedVersion = translationVersion) {
      TranslationLoader.getInstance(project).fetchAndApplyTranslation(course, translationLanguage)
    }

    // then
    coVerify(exactly = 1) { mockedService.getTranslatedCourse(course.id, course.marketplaceCourseVersion, translationLanguage) }

    val translationProperties = TranslationProjectSettings.getInstance(project).translationProperties.value
    val expectedTranslationProperties = TranslationProperties(
      language = translationLanguage,
      structureTranslation = mapOf(
        "2" to "lesson1 translated",
        "3" to "task1 translated",
        "4" to "task2 translated"
      ),
      version = TranslationVersion(translationVersion)
    )
    assertEquals(expectedTranslationProperties, translationProperties)

    checkFileTree {
      dir("lesson1") {
        dir("task1") {
          file("task.md")
          file("task_ru.md", "description 1 translated")
        }
        dir("task2") {
          file("task.md")
          file("task_ru.md", "description 2 translated")
        }
      }
    }
  }

  private fun performAndWait(expectedVersion: Int, action: () -> Unit) {
    val updated = AtomicBoolean(false)

    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch {
      TranslationProjectSettings.getInstance(project).translationProperties.collectLatest {
        if (it?.version?.value == expectedVersion) {
          updated.set(true)
        }
      }
    }
    try {
      action()
      waitFor { updated.get() }
    }
    finally {
      job.cancel()
    }
  }
}
