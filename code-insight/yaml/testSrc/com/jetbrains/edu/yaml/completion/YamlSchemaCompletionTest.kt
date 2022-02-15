package com.jetbrains.edu.yaml.completion

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CourseMode
import kotlin.test.assertNotEquals

class YamlSchemaCompletionTest : YamlCompletionTestBase() {
  fun `test completion for course programming language`() {
    courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: <caret>Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertEquals(2, lookupElements.size)
    assertContainsElements(lookupElements.map { it.lookupString }, FakeGradleBasedLanguage.displayName,
                           PlainTextLanguage.INSTANCE.displayName)
  }

  fun `test completion for course programming language version`() {
    courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: <caret>1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertEquals(1, lookupElements.size)
    assertContainsElements(lookupElements.map { it.lookupString }, "1.42")
  }

  fun `test completion for course human language`() {
    courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: <caret>Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertNotEquals(0, lookupElements.size)
    assertContainsElements(lookupElements.map { it.lookupString }, "English", "Russian", "German")
  }

  fun `test completion for course environment`() {
    courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: <caret>Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertEquals(2, lookupElements.size)
    assertContainsElements(lookupElements.map { it.lookupString }, "Android")
  }

  fun `test no completion in non-config file`() {
    myFixture.configureByText("random.yaml", """
      |title: Test Course
      |type: coursera
      |language: <caret>Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertEmpty(lookupElements)
  }

  fun `test no completion for is_template_based property for common lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
      lesson("lesson1")
    }
    val lesson = course.getLesson("lesson1")!!

    checkNoCompletion(lesson, """
      |is_templa<caret>
    """.trimMargin())
  }

  fun `test is_template_based property completion for framework lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
      frameworkLesson("lesson1")
    }
    val lesson = course.getLesson("lesson1")!!

    doSingleCompletion(lesson, """
      |type: framework
      |is_templa<caret>
    """.trimMargin(), """
      |type: framework
      |is_template_based: <selection>false</selection>
    """.trimMargin())
  }
}
