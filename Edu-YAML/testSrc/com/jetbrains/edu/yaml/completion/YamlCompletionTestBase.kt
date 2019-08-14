package com.jetbrains.edu.yaml.completion

import com.intellij.codeInsight.completion.CompletionType
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.yaml.YamlCodeInsightTest

abstract class YamlCompletionTestBase : YamlCodeInsightTest() {

  protected fun doSingleCompletion(item: StudyItem, before: String, after: String, invocationCount: Int = 1) {
    openConfigFileWithText(item, before)
    val variants = myFixture.complete(CompletionType.BASIC, invocationCount)
    if (variants != null) {
      if (variants.size == 1) {
        myFixture.type('\n')
        return
      }
      error("Expected a single completion, but got ${variants.size}\n" + variants.joinToString("\n") { it.lookupString })
    }
    myFixture.checkResult(after)
  }

  protected fun checkNoCompletion(item: StudyItem, text: String) {
    openConfigFileWithText(item, text)
    val variants = myFixture.completeBasic()
    checkNotNull(variants) { "Expected zero completions, but one completion was auto inserted" }
    assertEquals("Expected zero completions", 0, variants.size)
  }
}
