package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.course
import java.util.*

class CoursesInfosStorageTest : CoursesInfosStorageTestBase() {

  fun testDeserializeHumanLanguageInRussianLocale_old() {
    val default = Locale.getDefault()
    Locale.setDefault(Locale("ru", "RU"))
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    course.humanLanguage
    Locale.setDefault(default)
    assertEquals("en", course.languageCode)
  }

  fun testDeserializeHumanLanguageInEnglishLocale_old() {
    val default = Locale.getDefault()
    Locale.setDefault(Locale("en", ""))
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    course.humanLanguage
    Locale.setDefault(default)
    assertEquals("en", course.languageCode)
  }


  fun testSerializeHumanLanguage_old() {
    val default = Locale.getDefault()
    Locale.setDefault(Locale("ru", "RU"))
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book"
    ) { }.apply {
      id = 20403
      languageId = EduNames.PYTHON
      languageVersion = "3.7"
      languageCode = "ru"
    }

    Locale.setDefault(default)
    doSerializationTest(course)
  }
}