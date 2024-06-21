package com.jetbrains.edu.jarvis.grammar

import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.namedFunctionRegex
import com.jetbrains.edu.jarvis.enums.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase

class RegexTest : EduTestCase() {

  private fun validNamedFunctions() =
    List(NUMBER_OF_RUNS) {
      generateNamedFunction(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random(),
        List((MIN_NUMBER_OF_ARGS..MAX_NUMBER_OF_ARGS).random()) {
          generateIdentifier((MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random())
        }
      )
  } + listOf(
    "foo(1, abc, 2)",
    "bar()",
    "buzz(\"hi\")",
    "test1(a1,b1,c1)",
    "test2(a2)",
    "_test3(\"string\", variable, 123)",
  )

  private fun invalidNamedFunctions() =
    listOf(
    "123test()",
    "test (a, b, c",
    "test(a,b,)",
    "test(a b c)",
    "test(a,,b,c)",
    "test(a,b c)",
    "test",
    "(test)",
    "(test",
    "test)",
    "()",
    "test!(2)"
  )

  private fun validNoParenthesesFunctions() =
    List(NUMBER_OF_RUNS) {
      generateNoParenthesesFunction(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random()
      )
    } + listOf(
      TestAnswer("call the function `foo`","foo"),
      TestAnswer("InVOke `bar`", "bar"),
      TestAnswer("run the `buzz`", "buzz"),
      TestAnswer("call `print`","print"),
      TestAnswer("execute ThE function `welcome`","welcome"),
      TestAnswer("run the `calculate`","calculate"),
      TestAnswer("ExECUte `display`","display"),
      TestAnswer("call the `exit`","exit"),
      TestAnswer("Invoke the Function `start`","start"),
      TestAnswer("Run `stop`","stop"),
      TestAnswer("ruN THE  `foo`","foo"),
    )

  private fun invalidNoParenthesesFunctions() =
    listOf(
    "call `123test`",
    "run the `test (a, b, c`",
    "run the `test(a,,b,c)`",
    "execute `test(a,b c)`",
    "call `test(a,b,c,)`",
    "print `foo`",
    "run bar",
    "invoke the foo",
  )

  private fun validVariableDeclarations() =
    List(NUMBER_OF_RUNS) {
      generateVariableDeclaration(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random()
      )
    } + listOf(
      TestAnswer("Create `foo`", "foo"),
      TestAnswer("declare `bar`", "bar"),
      TestAnswer("Set `buzz`", "buzz"),
      TestAnswer("CreaTe the Variable `test1`", "test1"),
      TestAnswer("Set `test2`", "test2"),
      TestAnswer("Set the variable `_test3`", "_test3"),
    )

  private fun invalidVariableDeclarations() =
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

  fun testNamedFunctionRegex() {
    validNamedFunctions().forEach {
      assertTrue(namedFunctionRegex.matches(it))
    }
    invalidNamedFunctions().forEach {
      assertFalse(namedFunctionRegex.matches(it))
    }
  }

  fun testNoParenthesesFunctionRegex() {
    val regex = AnnotatorRule.NO_PARENTHESES_FUNCTION.regex
    validNoParenthesesFunctions().forEach {
      assertTrue(regex.matches(it.input))
      assertTrue(regex.find(it.input)!!.groups[1]!!.value == it.answer)
    }
    invalidNoParenthesesFunctions().forEach {
      assertFalse(regex.matches(it))
    }
  }

  fun testVariableDeclarationRegex() {
    val regex = AnnotatorRule.VARIABLE_DECLARATION.regex
    validVariableDeclarations().forEach {
      assertTrue(regex.matches(it.input))
      assertTrue(regex.find(it.input)!!.groups[1]!!.value == it.answer)
    }
    invalidVariableDeclarations().forEach {
      assertFalse(regex.matches(it))
    }
  }

}
