package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import org.junit.Test

class CoursesInfosStorageTest : CoursesInfosStorageTestBase() {

  @Test
  fun testSerializeCourseWithDefaultParameters() {
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book"
    ) { }.apply {
      id = 20403
      languageId = EduFormatNames.PYTHON
    }

    doSerializationTest(course)
  }

  @Test
  fun testSerializeLanguageVersion() {
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book"
    ) { }.apply {
      id = 20403
      languageId = EduFormatNames.PYTHON
      languageVersion = "3.7"
    }

    doSerializationTest(course)
  }
}