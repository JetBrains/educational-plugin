package com.jetbrains.edu.yaml.completion

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test
import kotlin.test.assertNotEquals

class YamlSchemaCompletionTest : YamlCompletionTestBase() {
  @Test
  fun `test completion for course programming language`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test completion for course programming language version`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test completion for course human language`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test completion for course environment`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test completion for disabled_features`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |content:
      |- lesson1
      |di<caret>
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertNotNull(lookupElements)
    assertContainsElements(lookupElements.map { it.lookupString }, "disabled_features")
  }

  @Test
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

  @Test
  fun `test no completion for is_template_based property for common lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1")
    }
    val lesson = course.getLesson("lesson1")!!

    checkNoCompletion(lesson, """
      |is_templa<caret>
    """.trimMargin())
  }

  @Test
  fun `test is_template_based property completion for framework lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
