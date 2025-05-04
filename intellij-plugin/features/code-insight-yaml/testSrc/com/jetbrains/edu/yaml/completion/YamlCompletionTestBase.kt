package com.jetbrains.edu.yaml.completion

import com.jetbrains.edu.codeInsight.EduCompletionTextFixture
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.yaml.YamlCodeInsightTest

abstract class YamlCompletionTestBase : YamlCodeInsightTest() {

  private lateinit var completionFixture: EduCompletionTextFixture

  override fun setUp() {
    super.setUp()
    completionFixture = EduCompletionTextFixture(myFixture)
    completionFixture.setUp()
  }

  override fun tearDown() {
    try {
      completionFixture.tearDown()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  protected fun doSingleCompletion(item: StudyItem, before: String, after: String, invocationCount: Int = 1) {
    openConfigFileWithText(item, before)
    completionFixture.doSingleCompletion(after, invocationCount)
  }

  protected fun checkNoCompletion(item: StudyItem, text: String) {
    openConfigFileWithText(item, text)
    completionFixture.checkNoCompletion()
  }
}
