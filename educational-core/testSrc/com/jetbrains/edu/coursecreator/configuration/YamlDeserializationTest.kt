package com.jetbrains.edu.coursecreator.configuration

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.Course


class YamlDeserializationTest: EduTestCase() {
  fun `test course`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"
    val yamlContent = """
      |title: $name
      |language: $language
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: $programmingLanguage
      |content:
      |- $firstLesson
      |- $secondLesson
      |""".trimMargin("|")
    val course = YamlFormatSynchronizer.MAPPER.readValue(yamlContent, Course::class.java)
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById.displayName)
    assertNotNull(course.description)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
  }
}