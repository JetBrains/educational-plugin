package com.jetbrains.edu.codeInsight

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import com.jetbrains.edu.learning.document

class EduCompletionTextFixture(
  private val fixture: CodeInsightTestFixture
) : BaseFixture() {

  fun doSingleCompletion(file: VirtualFile, before: String, after: String, invocationCount: Int = 1) {
    fixture.saveText(file, before.trimIndent())
    fixture.openFileInEditor(file)
    val caretsState = EditorTestUtil.extractCaretAndSelectionMarkers(file.document)
    EditorTestUtil.setCaretsAndSelection(fixture.editor, caretsState)

    doSingleCompletion(after, invocationCount)
  }

  fun doSingleCompletion(after: String, invocationCount: Int) {
    val variants = fixture.complete(CompletionType.BASIC, invocationCount)
    if (variants != null) {
      if (variants.size == 1) {
        fixture.type('\n')
        return
      }
      error("Expected a single completion, but got ${variants.size}\n" + variants.joinToString("\n") { it.lookupString })
    }
    fixture.checkResult(after.trimIndent())
  }

  fun checkNoCompletion() {
    val variants = fixture.completeBasic()
    checkNotNull(variants) { "Expected zero completions, but one completion was auto inserted" }
    BasePlatformTestCase.assertEquals("Expected zero completions", 0, variants.size)
  }
}
