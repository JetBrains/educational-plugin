package com.jetbrains.edu.codeInsight

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import kotlin.test.assertEquals

class EduCompletionTextFixture(
  private val fixture: CodeInsightTestFixture
) : BaseFixture() {

  fun doSingleCompletion(file: VirtualFile, before: String, after: String, invocationCount: Int = 1) {
    configureExistingFile(file, before)
    doSingleCompletion(after, invocationCount)
  }

  fun doSingleCompletion(after: String, invocationCount: Int) {
    val variants = fixture.complete(CompletionType.BASIC, invocationCount)
    if (variants != null) {
      if (variants.size == 1) {
        fixture.type('\n')
      }
      else {
        error("Expected a single completion, but got ${variants.size}\n" + variants.joinToString("\n") { it.lookupString })
      }
    }
    fixture.checkResult(after.trimIndent())
  }

  fun checkNoCompletion(file: VirtualFile, before: String) {
    configureExistingFile(file, before)
    checkNoCompletion()
  }

  fun checkNoCompletion() {
    val variants = fixture.completeBasic()
    checkNotNull(variants) { "Expected zero completions, but one completion was auto inserted" }
    BasePlatformTestCase.assertEquals("Expected zero completions", 0, variants.size)
  }

  fun checkDoNotContainCompletion(file: VirtualFile, before: String, variant: String) {
    configureExistingFile(file, before)
    // Invoke `completeBasic` under `withNoAutoCompletion` to prevent auto completion in case of single option
    val lookups = withNoAutoCompletion {
      fixture.completeBasic()
    }
    val renderedLookups = lookups.map { it.lookupString }
    check(variant !in renderedLookups) {
      "Expected completion list doesn't contain `$variant` option. Shown options: $renderedLookups"
    }
  }

  fun checkMultipleCompletion(vararg expectedVariants: String) {
    val actualVariants = withNoAutoCompletion {
      fixture.completeBasic()
    }
    assertEquals(
      expectedVariants.toSet(),
      actualVariants.map { it.lookupString }.toSet(),
      "The set of available completions is different",
    )
  }

  private fun configureExistingFile(file: VirtualFile, before: String) {
    fixture.saveText(file, before.trimIndent())
    fixture.configureFromExistingVirtualFile(file)
  }

  private fun <T> withNoAutoCompletion(block: () -> T): T {
    val prevSetting = CodeInsightSettings.getInstance().AUTOCOMPLETE_ON_CODE_COMPLETION
    CodeInsightSettings.getInstance().AUTOCOMPLETE_ON_CODE_COMPLETION = false
    return try {
      block()
    }
    finally {
      CodeInsightSettings.getInstance().AUTOCOMPLETE_ON_CODE_COMPLETION = prevSetting
    }
  }
}
