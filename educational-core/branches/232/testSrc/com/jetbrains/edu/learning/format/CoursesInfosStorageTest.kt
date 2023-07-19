package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.course

class CoursesInfosStorageTest : CoursesInfosStorageTestBase() {

  fun testSerializeCourseWithDefaultParameters() {
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book"
    ) { }.apply {
      id = 20403
      languageId = EduNames.PYTHON
    }

    doSerializationTest(course)
  }

  fun testSerializeLanguageVersion() {
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book"
    ) { }.apply {
      id = 20403
      languageId = EduNames.PYTHON
      languageVersion = "3.7"
    }

    doSerializationTest(course)
  }
}