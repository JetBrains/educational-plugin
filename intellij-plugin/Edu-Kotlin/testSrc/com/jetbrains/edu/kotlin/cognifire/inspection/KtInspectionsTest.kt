package com.jetbrains.edu.kotlin.cognifire.inspection

import com.jetbrains.edu.cognifire.inspection.InspectionProcessor
import com.jetbrains.edu.learning.EduTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtInspectionsTest : EduTestCase() {

  @Test
  fun testLiftReturnInspection() {
    val code = """
      fun foo(arg: Int): String {     
          when (arg) {         
              0 -> return "Zero"
              1 -> return "One"         
              else -> return "Multiple"     
          }
      }
    """.trimIndent()
    val expected = """
      fun foo(arg: Int): String {
          return when (arg) {
              0 -> "Zero"
              1 -> "One"
              else -> "Multiple"
          }
      }
    """.trimIndent()
    assertEquals(expected, InspectionProcessor.applyInspections(code, project, language))
  }

  @Test
  fun testLiftAssignmentInspection() {
    val code = """
      fun calculate(x: Int, y: Int) {
          var result: Int
          when {
              x > y -> result = x + 10
              x == y -> result = x + 5
              else -> result = x + 1
          }
      }
    """.trimIndent()
    val expected = """
      fun calculate(x: Int, y: Int) {
          var result: Int
          result = when {
              x > y -> x + 10
              x == y -> x + 5
              else -> x + 1
          }
      }
    """.trimIndent()
    assertEquals(expected, InspectionProcessor.applyInspections(code, project, language))
  }

  @Test
  fun testIntroduceWhenSubjectInspection() {
    val code = """
      fun test(obj: Any): String {
          return when {
              obj is String -> "string"
              obj is Int -> "int"
              else -> "unknown"
          }
      }
    """.trimIndent()
    val expected = """
      fun test(obj: Any): String {
          return when (obj) {
              is String -> "string"
              is Int -> "int"
              else -> "unknown"
          }
      }
    """.trimIndent()
    assertEquals(expected, InspectionProcessor.applyInspections(code, project, language))
  }

  @Test
  fun testIfThenToSafeAccessInspection() {
    val code = """
      fun bar(x: String) = ""

      fun foo(a: String?) {
         if (a != null) { 
            bar(a)
         }
      }
    """.trimIndent()
    val expected = """
      fun bar(x: String) = ""

      fun foo(a: String?) {
          a?.let { bar(it) }
      }
    """.trimIndent()
    assertEquals(expected, InspectionProcessor.applyInspections(code, project, language))
  }

  @Test
  fun testIfThenToElvisInspection() {
    val code = """
      fun maybeFoo(): String? = "foo"
      val foo = maybeFoo()
      val bar = if (foo == null) { 
          "hello"
      } else { 
          foo
      }
    """.trimIndent()
    val expected = """
      fun maybeFoo(): String? = "foo"
      val foo = maybeFoo()
      val bar = foo ?: "hello"
    """.trimIndent()
    assertEquals(expected, InspectionProcessor.applyInspections(code, project, language))
  }

  @Test
  fun testFoldInitializerAndIfToElvisInspection() {
    val code = """
      fun test(foo: Int?, bar: Int): Int {
          var i = foo
          if (i == null) {
              return bar
          }
          return i
      }
    """.trimIndent()
    val expected = """
      fun test(foo: Int?, bar: Int): Int {
          var i = foo ?: return bar
          return i
      }
    """.trimIndent()
    assertEquals(expected, InspectionProcessor.applyInspections(code, project, language))
  }

  @Test
  fun testJoinDeclarationAndAssignmentInspection() {
    val code = """
      fun foo() {
          val x: String
          x = System.getProperty("")
      }
    """.trimIndent()
    val expected = """
      fun foo() {
          val x: String = System.getProperty("")
      }
    """.trimIndent()
    assertEquals(expected, InspectionProcessor.applyInspections(code, project, language))
  }

  @Test
  fun testCascadeIfInspection() {
    val code = """
      fun checkIdentifier(id: String) {
          fun Char.isIdentifierStart() = this in 'A'..'z'
          fun Char.isIdentifierPart() = isIdentifierStart() || this in '0'..'9'
      
          if (id.isEmpty()) {
              print("Identifier is empty")
          } else if (!id.first().isIdentifierStart()) {
              print("Identifier should start with a letter")
          } else if (!id.subSequence(1, id.length).all(Char::isIdentifierPart)) {
              print("Identifier should contain only letters and numbers")
          }
      }
    """.trimIndent()
    val expected = """
      fun checkIdentifier(id: String) {
          fun Char.isIdentifierStart() = this in 'A'..'z'
          fun Char.isIdentifierPart() = isIdentifierStart() || this in '0'..'9'

          when {
              id.isEmpty() -> {
                  print("Identifier is empty")
              }
              !id.first().isIdentifierStart() -> {
                  print("Identifier should start with a letter")
              }
              !id.subSequence(1, id.length).all(Char::isIdentifierPart) -> {
                  print("Identifier should contain only letters and numbers")
              }
          }
      }
    """.trimIndent()
    assertEquals(expected, InspectionProcessor.applyInspections(code, project, language))
  }

  @Test
  fun testUnnecessaryVariableInspection() {
    val code = """
      fun sum(a: Int, b: Int): Int {
          val c = a + b
          return c
      }
    """.trimIndent()
    val expected = """
      fun sum(a: Int, b: Int): Int {
          return a + b
      }
    """.trimIndent()
    assertEquals(expected, InspectionProcessor.applyInspections(code, project, language))
  }

  companion object {
    private val language = KotlinLanguage.INSTANCE
  }
}
