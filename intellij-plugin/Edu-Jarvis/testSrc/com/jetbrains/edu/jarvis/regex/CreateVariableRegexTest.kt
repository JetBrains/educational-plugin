package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.NUMBER_OF_RUNS
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.generateVariableDeclaration
import com.jetbrains.edu.learning.EduTestCase

class CreateVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.CREATE_VARIABLE.regex

  override fun shouldMatch(): List<String> = emptyList()

  override fun shouldMatchGroup() =
    List(NUMBER_OF_RUNS) {
      generateVariableDeclaration(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random()
      )
    } + listOf(
      TestAnswer("Create `foo`", "foo"),
      TestAnswer("declare `bar`", "bar"),
      TestAnswer("CreaTe the Variable `test1`", "test1"),
      TestAnswer("Create an empty string `bar`", "bar")
    )

  override fun shouldNotMatch() =
  listOf(
    "create `foo()`",
    "run the `test`",
    "store the `test",
    "store the `2test2`",
    "call `test(a,b,c,)`",
    "print `foo`",
    "declare bar",
    "invoke the foo",
  )

  fun testValidNamedFunctions() = testShouldMatchGroup()
  fun testInvalidNamedFunctions() = testShouldNotMatch()

}
