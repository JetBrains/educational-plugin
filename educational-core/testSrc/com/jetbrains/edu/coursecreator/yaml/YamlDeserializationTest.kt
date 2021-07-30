package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.intellij.testFramework.exceptionCases.AbstractExceptionCase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeCourse
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeLesson
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeSection
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeTask
import com.jetbrains.edu.learning.yaml.YamlDeserializer.getCourseMode
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.MAPPER
import com.jetbrains.edu.learning.yaml.YamlTestCase
import java.util.*


class YamlDeserializationTest : YamlTestCase() {

  fun `test course`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val solutionsHidden = true
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"
    val yamlContent = """
      |title: $name
      |language: $language
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: $programmingLanguage
      |solutions_hidden: $solutionsHidden
      |content:
      |- $firstLesson
      |- $secondLesson
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById!!.displayName)
    assertEquals(solutionsHidden, course.solutionsHidden)
    assertNull(course.languageVersion)
    assertNotNull(course.description)
    assertEquals(EduNames.DEFAULT_ENVIRONMENT, course.environment)
    assertTrue(course is EduCourse)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
    assertFalse(course.isMarketplace)
  }

  fun `test course with no content`() {
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertTrue(course is EduCourse)
    assertFalse(course.solutionsHidden)
    assertEmpty(course.items)
  }

  fun `test course with environment`() {
    val environment = EduNames.ANDROID
    val yamlContent = """
      |title: Test Course
      |language: English
      |programming_language: Plain text
      |summary: |-
      |  This is a course about string theory.
      |environment: $environment
      |content:
      |- lesson1
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertEquals(environment, course.environment)
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
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent) as CourseraCourse
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById!!.displayName)
    assertFalse(course.submitManually)
    assertNotNull(course.description)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
    assertFalse(course.isMarketplace)
  }

  fun `test course with type`() {
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
      |""".trimMargin()
    val course = MAPPER.deserializeCourse(yamlContent)
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById!!.displayName)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
    assertFalse(course.isMarketplace)
  }

  fun `test course with language version`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val programmingLanguageVersion = "1.42"
    val yamlContent = """
      |title: $name
      |language: $language
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: $programmingLanguage
      |programming_language_version: $programmingLanguageVersion
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertEquals(programmingLanguage, course.languageById!!.displayName)
    assertEquals(programmingLanguageVersion, course.languageVersion)
  }

  fun `test section`() {
    val firstLesson = "Introduction Lesson"
    val secondLesson = "Advanced Lesson"
    val yamlContent = """
      |content:
      |- $firstLesson
      |- $secondLesson
    """.trimMargin()
    val section = MAPPER.deserializeSection(yamlContent)
    assertEquals(listOf(firstLesson, secondLesson), section.items.map { it.name })
  }

  fun `test section with custom presentable name`() {
    val firstLesson = "Introduction Lesson"
    val secondLesson = "Advanced Lesson"
    val customSectionName = "custom section name"
    val yamlContent = """
      |custom_name: $customSectionName
      |content:
      |- $firstLesson
      |- $secondLesson
    """.trimMargin()
    val section = MAPPER.deserializeSection(yamlContent)
    assertEquals(listOf(firstLesson, secondLesson), section.items.map { it.name })
    @Suppress("DEPRECATION")
    assertEquals(customSectionName, section.customPresentableName)
  }

  fun `test lesson`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin()
    val lesson = MAPPER.deserializeLesson(yamlContent)
    assertEquals(listOf(firstTask, secondTask), lesson.taskList.map { it.name })
  }

  fun `test lesson with custom presentable name`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val lessonCustomName = "my best lesson"
    val yamlContent = """
      |custom_name: $lessonCustomName
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin()
    val lesson = MAPPER.deserializeLesson(yamlContent)
    assertEquals(listOf(firstTask, secondTask), lesson.taskList.map { it.name })
    @Suppress("DEPRECATION")
    assertEquals(lessonCustomName, lesson.customPresentableName)
  }

  fun `test lesson with explicit type`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |type: lesson
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin()
    val lesson = MAPPER.deserializeLesson(yamlContent)
    assertEquals(listOf(firstTask, secondTask), lesson.taskList.map { it.name })
  }

  fun `test framework lesson`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |type: framework
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin()
    val lesson = MAPPER.deserializeLesson(yamlContent)
    check(lesson is FrameworkLesson)
    assertEquals(listOf(firstTask, secondTask), lesson.taskList.map { it.name })
    assertTrue(lesson.isTemplateBased)
  }

  fun `test empty framework lesson`() {
    val yamlContent = """
      |type: framework
      |
    """.trimMargin()
    val lesson = MAPPER.deserializeLesson(yamlContent)
    assertTrue(lesson is FrameworkLesson)
  }

  fun `test non templated based framework lesson`() {
    val yamlContent = """
      |type: framework
      |is_template_based: false
    """.trimMargin()
    val lesson = MAPPER.deserializeLesson(yamlContent)
    check(lesson is FrameworkLesson)
    assertFalse(lesson.isTemplateBased)
  }

  fun `test output task`() {
    val yamlContent = """
    |type: output
    |solution_hidden: false
    |files:
    |- name: Test.java
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is OutputTask)
    assertEquals(listOf("Test.java"), task.taskFiles.map { it.key })
    assertEquals(false, task.solutionHidden)
  }

  fun `test ide task`() {
    val yamlContent = """
    |type: ide
    |solution_hidden: true
    |files:
    |- name: Test.java
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is IdeTask)
    assertEquals(listOf("Test.java"), task.taskFiles.map { it.key })
    assertEquals(true, task.solutionHidden)
  }

  fun `test choice task`() {
    val correct = "correct"
    val incorrect = "incorrect"
    val yamlContent = """
      |type: choice
      |files:
      |- name: Test.java
      |  visible: true
      |is_multiple_choice: false
      |message_correct: $correct
      |message_incorrect: $incorrect
      |options:
      |- text: 1
      |  is_correct: true
      |- text: 2
      |  is_correct: false
      |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is ChoiceTask)
    assertEquals(listOf("Test.java"), task.taskFiles.map { it.key })
    assertEquals(mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT),
                 (task as ChoiceTask).choiceOptions.associateBy({ it.text }, { it.status }))
    assertEquals(correct, task.messageCorrect)
    assertEquals(incorrect, task.messageIncorrect)
  }

  fun `test choice task without answers`() {
    val yamlContent = """
      |type: choice
      |files:
      |- name: Test.java
      |  visible: true
      |is_multiple_choice: false
      |options:
      |- text: 1
      |- text: 2
      |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is ChoiceTask)
    assertEquals(listOf("Test.java"), task.taskFiles.map { it.key })
    assertEquals(mapOf("1" to ChoiceOptionStatus.UNKNOWN, "2" to ChoiceOptionStatus.UNKNOWN),
                 (task as ChoiceTask).choiceOptions.associateBy({ it.text }, { it.status }))
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
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
    assertEquals(3, answerPlaceholder.length)
    assertEquals("type here", answerPlaceholder.placeholderText)
    assertNull(task.solutionHidden)
  }

  fun `test with custom presentable name`() {
    val customName = "custom name"
    val yamlContent = """
    |type: edu
    |custom_name: $customName
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    @Suppress("DEPRECATION")
    assertEquals(customName, task.customPresentableName)
  }

  fun `test empty placeholder`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: ""
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
    assertEquals("", answerPlaceholder.placeholderText)
  }

  fun `test placeholder starts with spaces`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: '   type here'
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
    assertEquals("   type here", answerPlaceholder.placeholderText)
  }

  fun `test placeholder ends with spaces`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: 'type here   '
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
    assertEquals("type here   ", answerPlaceholder.placeholderText)
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
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
    assertEquals(3, answerPlaceholder.length)
    assertEquals("type here", answerPlaceholder.placeholderText)
  }

  fun `test feedback link`() {
    val yamlContent = """
    |type: edu
    |feedback_link: http://example.com
    |files:
    |- name: Test.java
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals("http://example.com", task.feedbackLink.link)
    assertEquals(FeedbackLink.LinkType.CUSTOM, task.feedbackLink.type)
  }

  fun `test vendor with email`() {
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |vendor:
      |  name: Jetbrains
      |  email: academy@jetbrains.com
      |programming_language: Plain text
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertTrue(course is EduCourse)
    val vendor = course.vendor
    assertNotNull(vendor)
    assertEquals("Jetbrains", vendor.name)
    assertEquals("academy@jetbrains.com", vendor.email)
    assertNull(vendor.url)
  }

  fun `test vendor with url`() {
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |vendor:
      |  name: Jetbrains
      |  url: jetbrains.com
      |programming_language: Plain text
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertTrue(course is EduCourse)
    val vendor = course.vendor
    assertNotNull(vendor)
    assertEquals("Jetbrains", vendor.name)
    assertEquals("jetbrains.com", vendor.url)
    assertNull(vendor.email)
  }

  fun `test isMarketplace`() {
    val yamlContent = """
      |type: marketplace
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertTrue(course is EduCourse)
    assertTrue(course.isMarketplace)
  }

  fun `test private course`() {
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |is_private: true
      |programming_language: Plain text
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertTrue(course is EduCourse)
    assertTrue(course.isMarketplacePrivate)
  }

  fun `test public course`() {
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertTrue(course is EduCourse)
    assertFalse(course.isMarketplacePrivate)
  }

  fun `test courseVersion`() {
    val yamlContent = """
      |type: marketplace
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |course_version: 5
      |programming_language: Plain text
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertTrue(course is EduCourse)
    assertEquals(5, course.marketplaceCourseVersion)
  }

  fun `test default courseVersion`() {
    val yamlContent = """
      |type: marketplace
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertTrue(course is EduCourse)
    assertEquals(1, course.marketplaceCourseVersion)
  }

  fun `test file visibility`() {
    val taskFileName = "Task.java"
    val testFileName = "Test.java"
    val yamlContent = """
    |type: edu
    |files:
    |- name: $taskFileName
    |  visible: true
    |- name: $testFileName
    |  visible: false
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles[taskFileName]!!
    assertTrue("$taskFileName expected to be visible", taskFile.isVisible)
    val testFile = task.taskFiles[testFileName]!!
    assertTrue("$testFileName expected to be invisible", !testFile.isVisible)
  }

  fun `test empty edu task`() {
    val yamlContent = """
    |type: edu
    |""".trimMargin()
    val task = MAPPER.deserializeTask(yamlContent)
    assertEmpty(task.taskFiles.values)
  }

  fun `test empty lesson`() {
    val yamlContent = """
    |
    |{}
    |""".trimMargin()
    val lesson = MAPPER.deserializeLesson(yamlContent)
    assertTrue(lesson.taskList.isEmpty())
  }

  fun `test empty lesson with empty config`() {
    val yamlContent = """
    |""".trimMargin()
    val lesson = MAPPER.deserializeLesson(yamlContent)
    assertEmpty(lesson.taskList)
  }

  fun `test empty section`() {
    val yamlContent = """
    |
    |{}
    |""".trimMargin()
    val section = MAPPER.deserializeSection(yamlContent)
    assertTrue(section.lessons.isEmpty())
  }

  fun `test empty section with empty config`() {
    val yamlContent = """
    |""".trimMargin()
    val section = MAPPER.deserializeSection(yamlContent)
    assertEmpty(section.lessons)
  }

  fun `test coursera manual submit`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"
    val yamlContent = """
      |type: ${CourseraNames.COURSE_TYPE}
      |submit_manually: true
      |title: $name
      |language: $language
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: $programmingLanguage
      |content:
      |- $firstLesson
      |- $secondLesson
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent) as CourseraCourse
    assertTrue(course.submitManually)
  }

  fun `test non-english locale`() {
    val defaultLocale = Locale.getDefault()
    Locale.setDefault(Locale.KOREAN)
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |""".trimMargin()

    assertNoException(object: AbstractExceptionCase<InvalidDefinitionException>() {
      override fun getExpectedExceptionClass(): Class<InvalidDefinitionException> = InvalidDefinitionException::class.java

      override fun tryClosure() {
        deserializeNotNull(yamlContent)
      }
    })
    Locale.setDefault(defaultLocale)
  }

  fun `test cc mode`() {
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |""".trimMargin()

    assertNull(getCourseMode(yamlContent))
  }

  fun `test study mode`() {
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |mode: Study
      |""".trimMargin()

    assertEquals(EduNames.STUDY, getCourseMode(yamlContent))
  }

  private fun deserializeNotNull(yamlContent: String) : Course = MAPPER.deserializeCourse(yamlContent)
}