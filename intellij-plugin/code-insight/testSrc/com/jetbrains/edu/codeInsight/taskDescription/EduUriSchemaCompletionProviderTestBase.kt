package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.testFramework.fixtures.CompletionAutoPopupTester
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.junit.Test

abstract class EduUriSchemaCompletionProviderTestBase(format: DescriptionFormat) : EduTaskDescriptionCompletionTestBase(format) {

  private lateinit var tester: CompletionAutoPopupTester

  override fun setUp() {
    super.setUp()
    tester = CompletionAutoPopupTester(myFixture)
  }

  override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
    tester.runWithAutoPopupEnabled(testRunnable)
  }

  override fun runInDispatchThread(): Boolean = false

  @Test
  fun `test course schema`() = doTest("cour<caret>", "course://<caret>", popupExpected = true)
  @Test
  fun `test psi_element schema`() = doTest("psi<caret>", "psi_element://<caret>", popupExpected = false)
  @Test
  fun `test settings schema`() = doTest("sett<caret>", "settings://<caret>", popupExpected = true)
  @Test
  fun `test tool window schema`() = doTest("tool<caret>", "tool_window://<caret>", popupExpected = true)

  @Test
  fun `test complete in any part of schema 1`() = doTest("settings:<caret>", "settings://<caret>")
  @Test
  fun `test complete in any part of schema 2`() = doTest("tool_window:/<caret>", "tool_window://<caret>")

  @Test
  fun `test do not suggest schema if it is already typed`() = checkDoNotContainCompletion("settings://<caret>", "settings://")

  protected fun doTest(linkBefore: String, linkAfter: String, popupExpected: Boolean) {
    doTest(linkBefore, linkAfter)
    tester.joinAutopopup()
    tester.joinCommit()
    if (popupExpected) {
      assertNotNull(tester.lookup)
    }
    else {
      assertNull(tester.lookup)
    }
  }
}
