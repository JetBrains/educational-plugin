package com.jetbrains.edu.learning.marketplace.courseStorage

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState
import org.junit.Test

class CourseStorageMetadataProcessor : EduTestCase() {

  override fun setUp() {
    super.setUp()
    courseWithFiles(id = 1) {
      lesson("lesson1", id = 2) {
        eduTask("task1", stepId = 3)
        eduTask("task2", stepId = 4)
      }
    } as EduCourse
  }

  override fun tearDown() {
    try {
      CourseStorageLinkSettings.getInstance(project).link = null
      CourseStorageLinkSettings.getInstance(project).platformName = null
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test link is saved`() {
    val params = mapOf("link" to "https://${TRUSTED_COURSE_STORAGE_HOSTS.shuffled().first()}", "platformName" to "AWS")
    doTest(params)
    assertCorrect(params)
  }

  @Test
  fun `test link with untrusted host is not saved`() {
    val params = mapOf("link" to "https://google.com", "platformName" to "AWS")
    doTest(params)
    assertFails()
  }

  @Test
  fun `test link with invalid protocol is not saved`() {
    val params = mapOf("link" to "ftp://google.com", "platformName" to "AWS")
    doTest(params)
    assertFails()
  }

  @Test
  fun `test link with absent protocol is not saved`() {
    val params = mapOf("link" to "google.com", "platformName" to "AWS")
    doTest(params)
    assertFails()
  }

  @Test
  fun `test missing link parameter`() {
    val params = mapOf(
      "platformName" to "AWS"
    )
    doTest(params)
    assertFails()
  }

  @Test
  fun `test missing platform name parameter`() {
    val params = mapOf(
      "link" to "https://academy.jetbrains.com"
    )
    doTest(params)
    assertFails()
  }

  private fun doTest(params: Map<String, String>) {
    val course = project.course as EduCourse
    CourseMetadataProcessor.applyProcessors(project, course, params, courseProjectState = CourseProjectState.CREATED_PROJECT)
  }

  fun assertCorrect(params: Map<String, String>) {
    val courseStorageLinkSettings = CourseStorageLinkSettings.getInstance(project)
    assertEquals(params["link"], courseStorageLinkSettings.link)
    assertEquals(params["platformName"], courseStorageLinkSettings.platformName)
  }

  private fun assertFails() {
    val courseStorageLinkSettings = CourseStorageLinkSettings.getInstance(project)
    assertNull(courseStorageLinkSettings.link)
    assertNull(courseStorageLinkSettings.platformName)
  }
}