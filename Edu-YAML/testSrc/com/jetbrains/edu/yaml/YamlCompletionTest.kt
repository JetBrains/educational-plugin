package com.jetbrains.edu.yaml

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.yaml.skipYamlCompletionTests
import kotlin.test.assertNotEquals

class YamlCompletionTest : YamlCodeInsightTest() {

  // can't use existing shouldRunTest method because ApplicationInfo isn't initialized when it's called
  // and we can't check ide type
  override fun runTest() {
    // BACKCOMPAT: 2019.1 tests fail in Studio 191 and IJ 183
    @Suppress("ConstantConditionIf")
    if (!skipYamlCompletionTests) {
      super.runTest()
    }
  }

  fun `test completion for course programming language`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
}