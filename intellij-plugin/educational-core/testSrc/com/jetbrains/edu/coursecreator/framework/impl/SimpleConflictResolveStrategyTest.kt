package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.diff.SimpleConflictResolveStrategy
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class SimpleConflictResolveStrategyTest : EduTestCase() {
  private val conflictStrategy = SimpleConflictResolveStrategy()

  @Test
  fun `test simple conflict strategy`() {
    val baseState = mapOf(
      "a.kt" to "fun f() = 12",
      "b.kt" to "fun x() = 42",
    )
    val currentState = mapOf(
      "a.kt" to "fun f() = 42",
      "c.kt" to "fun ggg() = 0",
    )
    val targetState = mapOf(
      "a.kt" to "fun f() = 32",
      "d.kt" to "fun yyy() = 100",
    )
    val (conflictFiles, actualState) = conflictStrategy.resolveConflicts(currentState, baseState, targetState)
    assertEquals(listOf("a.kt"), conflictFiles)
    val expectedState = mapOf(
      "a.kt" to "fun f() = 12",
      "c.kt" to "fun ggg() = 0",
      "d.kt" to "fun yyy() = 100",
    )
    assertEquals(expectedState, actualState)
  }

  @Test
  fun `test conflict files during simple conflict strategy`() {
    val baseState = mapOf(
      "a.kt" to "a.kt",
      "b.kt" to "b.kt",
      "c.kt" to "c.kt",
    )
    val currentState = mapOf(
      "b.kt" to "b1.kt",
      "d.kt" to "d.kt",
    )
    val targetState = mapOf(
      "a.kt" to "a1.kt",
      "c.kt" to "cc.kt",
      "d.kt" to "dd.kt",
    )
    val (conflictFiles, actualState) = conflictStrategy.resolveConflicts(currentState, baseState, targetState)
    val sortedConflictFiles = conflictFiles.sorted()
    assertEquals(listOf("a.kt", "b.kt", "c.kt", "d.kt"), sortedConflictFiles)
    val expectedState = mapOf(
      "a.kt" to "a.kt",
      "b.kt" to "b.kt",
      "c.kt" to "c.kt",
      "d.kt" to "d.kt",
    )
    assertEquals(expectedState, actualState)
  }
}