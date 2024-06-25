package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase

class SetVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.SET_VARIABLE.regex

  override fun shouldMatch(): List<String> = emptyList()

  override fun shouldMatchGroup() =
    listOf(
      TestAnswer("Set `foo` to 3", "foo"),
      TestAnswer("set the variable `bar`", "bar"),
      TestAnswer("set `bar` to \"Hello world", "bar"),
      TestAnswer("set `myVar` to `foo` multiplied by 2", "myVar"),
      TestAnswer("initialize `myVar` to `foo` multiplied by `bar`", "myVar"),
    )

  override fun shouldNotMatch() =
    listOf(
      "create `foo()`",
      "run the `test`",
      "set the v t`test",
      "store the `2test2`",
      "call `test(a,b,c,)`",
      "seet `foo`",
      "se bar",
      "invoke the foo",
    )

  fun testValidNamedFunctions() = testShouldMatchGroup()
  fun testInvalidNamedFunctions() = testShouldNotMatch()

}
