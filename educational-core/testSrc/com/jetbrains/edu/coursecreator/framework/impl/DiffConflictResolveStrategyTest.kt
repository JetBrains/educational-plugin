package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.diff.DiffConflictResolveStrategy

class DiffConflictResolveStrategyTest : ConflictResolveStrategyTestBase<DiffConflictResolveStrategy>() {
  override val conflictStrategy
    get() = DiffConflictResolveStrategy(project)

  fun `test solve some unrelated changes`() {
    val baseState = mapOf(
      "a.kt" to """
        //TODO()
      """.trimIndent()
    )
    val currentState = mapOf(
      "a.kt" to """
        //TODO()
        
        fun main() { println("MEM") }
      """.trimIndent()
    )
    val targetState = mapOf(
      "a.kt" to """
        fun f() = 128
        
        //TODO()
      """.trimIndent()
    )

    val (conflictFiles, expectedState) = conflictStrategy.resolveConflicts(currentState, baseState, targetState)
    assertEquals(emptyList<String>(), conflictFiles)
    val actualState = mapOf(
      "a.kt" to """
        fun f() = 128
        
        //TODO()
        
        fun main() { println("MEM") }
      """.trimIndent()
    )
    assertEquals(expectedState, actualState)
  }

  fun `test do not resolve changes in conflict file`() {
    val baseState = mapOf("a.kt" to """
        //TODO()
      """.trimIndent()
    )
    val currentState = mapOf("a.kt" to """
        //TODO1()
        
        fun main() { println("MEM") }
      """.trimIndent(),
    )
    val targetState = mapOf(
      "a.kt" to """
        fun f() = 128
        
        //TODO2()
      """.trimIndent(),
    )

    val (conflictFiles, expectedState) = conflictStrategy.resolveConflicts(currentState, baseState, targetState)
    assertEquals(listOf("a.kt"), conflictFiles)
    val actualState = mapOf(
      "a.kt" to """
        //TODO()
      """.trimIndent()
    )
    assertEquals(expectedState, actualState)
  }

  fun `test resolve simple conflicts`() {
    val baseState = mapOf(
      "a.kt" to "TODO()"
    )
    val currentState = mapOf(
      "a.kt" to "TODO()",
      "c.kt" to "fun ggg() = 0",
    )
    val targetState = mapOf(
      "a.kt" to "TODO()",
      "d.kt" to "fun yyy() = 100",
    )

    val (conflictFiles, expectedState) = conflictStrategy.resolveConflicts(currentState, baseState, targetState)
    assertEquals(emptyList<String>(), conflictFiles)
    val actualState = mapOf(
      "a.kt" to "TODO()",
      "c.kt" to "fun ggg() = 0",
      "d.kt" to "fun yyy() = 100",
    )
    assertEquals(expectedState, actualState)
  }

  fun `test do not resolve deletion conflict`() {
    val baseState = mapOf(
      "a.kt" to """
        //TODO()
      """.trimIndent()
    )
    val currentState = mapOf(
      "a.kt" to """
        //TODO()
        
        fun main() { println("MEM") }
      """.trimIndent(),
    )
    val targetState = mapOf<String, String>()

    val (conflictFiles, expectedState) = conflictStrategy.resolveConflicts(currentState, baseState, targetState)
    assertEquals(listOf("a.kt"), conflictFiles)
    val actualState = mapOf(
      "a.kt" to """
        //TODO()
      """.trimIndent()
    )
    assertEquals(expectedState, actualState)
  }
}