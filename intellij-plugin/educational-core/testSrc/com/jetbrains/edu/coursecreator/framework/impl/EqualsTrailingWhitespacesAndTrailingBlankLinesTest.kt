package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.diff.equalsTrimTrailingWhitespacesAndTrailingBlankLines
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EqualsTrailingWhitespacesAndTrailingBlankLinesTest {
  @Test
  fun `test comparison returns true for equal strings`() {
    val a = """
      fun main() {
        println("hello world!")
      }
    """.trimIndent()
    val b = """
      fun main() {
        println("hello world!")
      }
    """.trimIndent()
    assertTrue { equalsTrimTrailingWhitespacesAndTrailingBlankLines(a, b) }
  }

  @Test
  fun `test comparison returns false for non-equal strings`() {
    val a = """
      fun main() {
        println("hello world!")
      }
    """.trimIndent()
    val b = """
      fun main() {
        println("no!")
      }
    """.trimIndent()
    assertFalse { equalsTrimTrailingWhitespacesAndTrailingBlankLines(a, b) }
  }

  @Test
  fun `test comparison ignores trailing spaces`() {
    val a = """
      fun main() {         
        println("hello world!") 
      }
    """.trimIndent()
    val b = "fun main() {         \n  println(\"hello world!\")     \n}    "
    assertTrue { equalsTrimTrailingWhitespacesAndTrailingBlankLines(a, b) }
  }

  @Test
  fun `test comparison does not ignore leading spaces`() {
    val a = """
      fun main() {
        println("hello world!")
      }
    """.trimIndent()
    val b = """
      fun main() {
      println("hello world!")
      }
    """.trimIndent()
    assertFalse { equalsTrimTrailingWhitespacesAndTrailingBlankLines(a, b) }
  }

  @Test
  fun `test comparison ignore trailing blank lines`() {
    val a = """
      fun main() {
        println("hello world!")   
      }
          
          
    """.trimIndent()
    val b = """
      fun main() {
        println("hello world!")
      }
          
    """.trimIndent()
    assertTrue { equalsTrimTrailingWhitespacesAndTrailingBlankLines(a, b) }
  }
}