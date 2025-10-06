package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.diff.resolveConflicts
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.assertContentsEqual
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import org.junit.Test

class DiffConflictResolveStrategyTest : EduTestCase() {
  @Test
  fun `test solve some unrelated changes`() {
    val baseState = stateOf(
      "a.kt" to """
        //TODO()
      """.trimIndent()
    )
    val currentState = stateOf(
      "a.kt" to """
        //TODO()
        
        fun main() { println("MEM") }
      """.trimIndent()
    )
    val targetState = stateOf(
      "a.kt" to """
        fun f() = 128
        
        //TODO()
      """.trimIndent()
    )

    val (conflictFiles, actualState) = resolveConflicts(project, currentState, baseState, targetState)
    assertEquals(emptyList<String>(), conflictFiles)
    val expectedState = stateOf(
      "a.kt" to """
        fun f() = 128
        
        //TODO()
        
        fun main() { println("MEM") }
      """.trimIndent()
    )
    assertStateEquals(expectedState, actualState)
  }

  @Test
  fun `test do not resolve changes in conflict file`() {
    val baseState = stateOf("a.kt" to """
        //TODO()
      """.trimIndent()
    )
    val currentState = stateOf("a.kt" to """
        //TODO1()
        
        fun main() { println("MEM") }
      """.trimIndent(),
    )
    val targetState = stateOf(
      "a.kt" to """
        fun f() = 128
        
        //TODO2()
      """.trimIndent(),
    )

    val (conflictFiles, actualState) = resolveConflicts(project, currentState, baseState, targetState)
    assertEquals(listOf("a.kt"), conflictFiles)
    val expectedState = stateOf(
      "a.kt" to """
        //TODO()
      """.trimIndent()
    )
    assertStateEquals(expectedState, actualState)
  }

  @Test
  fun `test resolve simple conflicts`() {
    val baseState = stateOf(
      "a.kt" to "TODO()"
    )
    val currentState = stateOf(
      "a.kt" to "TODO()",
      "c.kt" to "fun ggg() = 0",
    )
    val targetState = stateOf(
      "a.kt" to "TODO()",
      "d.kt" to "fun yyy() = 100",
    )

    val (conflictFiles, actualState) = resolveConflicts(project, currentState, baseState, targetState)
    assertEquals(emptyList<String>(), conflictFiles)
    val expectedState = stateOf(
      "a.kt" to "TODO()",
      "c.kt" to "fun ggg() = 0",
      "d.kt" to "fun yyy() = 100",
    )
    assertStateEquals(expectedState, actualState)
  }

  @Test
  fun `test do not resolve (deleted, modified) conflict`() {
    val baseState = stateOf(
      "a.kt" to """
        fun f() = 12

        //adjkfjkasdfjkasjkdfjkashdjk
        
      """.trimIndent()
    )
    val currentState = stateOf(
      "a.kt" to """
        fun f() = 12

        //adjkfjkasdfjkasjkdfjkashdjk
        
        val x = 12
      """.trimIndent(),
    )
    val targetState = stateOf()

    val (conflictFiles, actualState) = resolveConflicts(project, currentState, baseState, targetState)
    assertEquals(listOf("a.kt"), conflictFiles)
    val expectedState = stateOf(
      "a.kt" to """
        fun f() = 12

        //adjkfjkasdfjkasjkdfjkashdjk
        
      """.trimIndent()
    )
    assertStateEquals(expectedState, actualState)
  }

  @Test
  fun `test do not resolve (added, added) conflict`() {
    val currentState = stateOf(
      "a.kt" to """
        fun f() = 12

        //adjkfjkasdfjkasjkdfjkashdjk
        
      """.trimIndent()
    )
    val targetState = stateOf(
      "a.kt" to """

        //adjkfjkasdfjkasjkdfjkashdjk
        
        val x = 12
      """.trimIndent(),
    )
    val baseState = stateOf()

    val (conflictFiles, actualState) = resolveConflicts(project, currentState, baseState, targetState)
    assertEquals(listOf("a.kt"), conflictFiles)
    val expectedState = stateOf(
      "a.kt" to """
        fun f() = 12

        //adjkfjkasdfjkasjkdfjkashdjk
        
      """.trimIndent()
    )
    assertStateEquals(expectedState, actualState)
  }

  @Test
  fun `test resolve several conflicts in one file`() {
    val baseState = stateOf(
      "a.kt" to """
        fun f() = 12
        
        fun main() {
        
        }
      """.trimIndent()
    )
    val currentState = stateOf(
      "a.kt" to """
        fun f() = 12
        
        fun g() {
          println("GGGG")
          
          
        }
        
        fun main() {
        
        }
      """.trimIndent()
    )
    val targetState = stateOf(
      "a.kt" to """
        fun f() = 12
        
        fun main() {
          println("Hello world!")
          println(f())
        }
        
        fun asd() {
          return asd()
        }
      """.trimIndent()
    )

    val (conflictFiles, actualState) = resolveConflicts(project, currentState, baseState, targetState)
    assertEquals(emptyList<String>(), conflictFiles)
    val expectedState = stateOf(
      "a.kt" to """
        fun f() = 12
        
        fun g() {
          println("GGGG")
          
          
        }
        
        fun main() {
          println("Hello world!")
          println(f())
        }
        
        fun asd() {
          return asd()
        }
      """.trimIndent(),
    )
    assertStateEquals(expectedState, actualState)
  }

  @Test
  fun `test non-trivial conflicts in binary files does not resolve`() {
    val baseState = mapOf("a.kt" to InMemoryBinaryContents(byteArrayOf(0, 0)))
    val currentState = mapOf("a.kt" to InMemoryBinaryContents(byteArrayOf(1, 1)))
    val targetState = mapOf("a.kt" to InMemoryBinaryContents(byteArrayOf(1, 0)))

    val (conflictFiles, actualState) = resolveConflicts(project, currentState, baseState, targetState)
    assertEquals(listOf("a.kt"), conflictFiles)
    assertStateEquals(mapOf("a.kt" to InMemoryBinaryContents(byteArrayOf(0, 0))), actualState)
  }

  private fun assertStateEquals(expectedState: FLTaskState, actualState: FLTaskState) {
    assertEquals(expectedState.size, actualState.size)
    for ((path, expectedContents) in expectedState) {
      val actualContents = actualState[path]!!
      assertContentsEqual(path, expectedContents, actualContents)
    }
  }

  private fun stateOf(vararg files: Pair<String, String>): FLTaskState = files.toMap().mapValues { InMemoryTextualContents(it.value) }
}