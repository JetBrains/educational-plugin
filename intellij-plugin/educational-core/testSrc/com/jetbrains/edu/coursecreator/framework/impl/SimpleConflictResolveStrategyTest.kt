package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.diff.SimpleConflictResolveStrategy
import com.jetbrains.edu.coursecreator.framework.diff.resolveConflicts
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.assertContentsEqual
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import org.junit.Test

class SimpleConflictResolveStrategyTest : EduTestCase() {
  private val conflictStrategy = SimpleConflictResolveStrategy()

  @Test
  fun `test simple conflict strategy`() {
    val baseState = stateOf(
      "a.kt" to "fun f() = 12",
      "b.kt" to "fun x() = 42",
    )
    val currentState = stateOf(
      "a.kt" to "fun f() = 42",
      "c.kt" to "fun ggg() = 0",
    )
    val targetState = stateOf(
      "a.kt" to "fun f() = 32",
      "d.kt" to "fun yyy() = 100",
    )
    val (conflictFiles, actualState) = conflictStrategy.resolveConflicts(currentState, baseState, targetState)
    assertEquals(listOf("a.kt"), conflictFiles)
    val expectedState = stateOf(
      "a.kt" to "fun f() = 12",
      "c.kt" to "fun ggg() = 0",
      "d.kt" to "fun yyy() = 100",
    )
    assertStateEquals(expectedState, actualState)
  }

  @Test
  fun `test conflict files during simple conflict strategy`() {
    val baseState = stateOf(
      "a.kt" to "a.kt",
      "b.kt" to "b.kt",
      "c.kt" to "c.kt",
    )
    val currentState = stateOf(
      "b.kt" to "b1.kt",
      "d.kt" to "d.kt",
    )
    val targetState = stateOf(
      "a.kt" to "a1.kt",
      "c.kt" to "cc.kt",
      "d.kt" to "dd.kt",
    )
    val (conflictFiles, actualState) = conflictStrategy.resolveConflicts(currentState, baseState, targetState)
    val sortedConflictFiles = conflictFiles.sorted()
    assertEquals(listOf("a.kt", "b.kt", "c.kt", "d.kt"), sortedConflictFiles)
    val expectedState = stateOf(
      "a.kt" to "a.kt",
      "b.kt" to "b.kt",
      "c.kt" to "c.kt",
      "d.kt" to "d.kt",
    )
    assertStateEquals(expectedState, actualState)
  }

  @Test
  fun `test trivial conflicts in binary files resolves correctly`() {
    val baseState = mapOf("a.kt" to InMemoryBinaryContents(byteArrayOf(0, 0)))
    val currentState = mapOf("a.kt" to InMemoryBinaryContents(byteArrayOf(1, 1)))
    val targetState = mapOf("a.kt" to InMemoryBinaryContents(byteArrayOf(0, 0)))

    val (conflictFiles, actualState) = resolveConflicts(project, currentState, baseState, targetState)
    assertEquals(emptyList<String>(), conflictFiles)
    assertStateEquals(mapOf("a.kt" to InMemoryBinaryContents(byteArrayOf(1, 1))), actualState)
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