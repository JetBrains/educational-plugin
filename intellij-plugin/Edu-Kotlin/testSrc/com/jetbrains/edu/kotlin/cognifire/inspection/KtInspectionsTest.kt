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

  companion object {
    private val language = KotlinLanguage.INSTANCE
  }
}
