package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.Experiments
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse


class YamlDeserializationTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    Experiments.setFeatureEnabled(EduExperimentalFeatures.YAML_FORMAT, true)
  }

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
    val course = YamlDeserializer.deserialize(yamlContent, Course::class.java)
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById.displayName)
    assertNotNull(course.description)
    assertTrue(course is EduCourse)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
  }

  fun `test coursera course`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"
    val yamlContent = """
      |type: ${CourseraNames.COURSE_TYPE}
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
    val course = YamlDeserializer.deserialize(yamlContent, CourseraCourse::class.java)
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById.displayName)
    assertNotNull(course.description)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
  }

  fun `test checkio course`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"
    val yamlContent = """
      |type: ${CheckiONames.CHECKIO_TYPE}
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
    val course = YamlDeserializer.deserialize(yamlContent, CheckiOCourse::class.java)
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById.displayName)
    assertNotNull(course.description)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
  }

  fun `test stepik course`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"
    val yamlContent = """
      |type: ${StepikNames.STEPIK_TYPE}
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
    val course = YamlDeserializer.deserialize(yamlContent, StepikCourse::class.java)
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById.displayName)
    assertNotNull(course.description)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
  }

  fun `test hyperskill course`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"
    val yamlContent = """
      |type: ${HYPERSKILL_TYPE}
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
    val course = YamlDeserializer.deserialize(yamlContent, HyperskillCourse::class.java)
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById.displayName)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
  }

  fun `test section`() {
    val firstLesson = "Introduction Lesson"
    val secondLesson = "Advanced Lesson"
    val yamlContent = """
      |content:
      |- $firstLesson
      |- $secondLesson
    """.trimMargin("|")
    val section = YamlDeserializer.deserialize(yamlContent, Section::class.java)
    assertEquals(listOf(firstLesson, secondLesson), section.items.map { it.name })
  }

  fun `test lesson`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin("|")
    val lesson = YamlDeserializer.deserializeLesson(yamlContent)
    assertEquals(listOf(firstTask, secondTask), lesson.getTaskList().map { it.name })
  }

  fun `test framework lesson`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |type: framework
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin("|")
    val lesson = YamlDeserializer.deserializeLesson(yamlContent)
    assertTrue(lesson is FrameworkLesson)
    assertEquals(listOf(firstTask, secondTask), lesson.getTaskList().map { it.name })
  }

  fun `test output task`() {
    val yamlContent = """
    |type: output
    |files:
    |- name: Test.java
    |""".trimMargin("|")
    val task = YamlDeserializer.deserializeTask(yamlContent)
    assertTrue(task is OutputTask)
    assertEquals(listOf("Test.java"), task.taskFiles.map { it.key })
  }

  fun `test edu task`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    dependency:
    |      lesson: lesson1
    |      task: task1
    |      file: Test.java
    |      placeholder: 1
    |      is_visible: true
    |""".trimMargin("|")
    val task = YamlDeserializer.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
    assertEquals(9, answerPlaceholder.length)
    assertEquals(3, answerPlaceholder.possibleAnswer.length)
    assertEquals("lesson1#task1#Test.java#1", answerPlaceholder.placeholderDependency.toString())
  }

  fun `test edu task without dependency`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |""".trimMargin("|")
    val task = YamlDeserializer.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
    assertEquals(9, answerPlaceholder.length)
    assertEquals(3, answerPlaceholder.possibleAnswer.length)
  }

  fun `test feedback link`() {
    val yamlContent = """
    |type: edu
    |feedback_link: http://example.com
    |files:
    |- name: Test.java
    |""".trimMargin("|")
    val task = YamlDeserializer.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals("http://example.com", task.feedbackLink.link)
    assertEquals(FeedbackLink.LinkType.CUSTOM, task.feedbackLink.type)
  }
}