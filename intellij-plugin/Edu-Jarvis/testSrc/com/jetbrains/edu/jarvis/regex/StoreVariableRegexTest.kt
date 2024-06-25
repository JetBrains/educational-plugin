package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase

class StoreVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.STORE_VARIABLE.regex

  override fun shouldMatch(): List<String> = emptyList()

  override fun shouldMatchGroup() =
    listOf(
      TestAnswer("store the value 3 in `foo`", "foo"),
      TestAnswer("store 5 in `bar`", "bar"),
      TestAnswer("store `bar` in `foo2`", "foo2"),
      TestAnswer("store \"hello\" in `foo3`", "foo3"),
    )

  override fun shouldNotMatch() =
    listOf(
      "store `foo()`",
      "storee the `test3`",
      "store the `test",
      "store the `2test2`",
      "call `test(a,b,c,)`",
      "declare `foo`",
      "declare bar",
      "invoke the foo",
    )

  fun testValidNamedFunctions() = testShouldMatchGroup()
  fun testInvalidNamedFunctions() = testShouldNotMatch()

}
