package com.jetbrains.edu.learning.taskToolWindow

import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckMessagePanel.Companion.prepareHtmlText
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckMessagePanelTest {
  @Test
  fun `test message`() {
    val input = """Failed. Perhaps, you changed something in the code. Just copy it!"""
    val expected = """<html><head/><body>Failed. Perhaps, you changed something in the code. Just copy it!</body></html>"""
    doTest(input, expected)
  }

  @Test
  fun `test message wrapped with html tag`() {
    val input = """<html>Passed. Congratulations! It is your first Kotlin program that really works. In future topics, you will learn more about this code.</html>"""
    val expected = """<html>Passed. Congratulations! It is your first Kotlin program that really works. In future topics, you will learn more about this code.</html>"""
    doTest(input, expected)
  }

  private fun doTest(input: String, expected: String) {
    val actual = prepareHtmlText(input)
    assertEquals(expected, actual)
  }
}