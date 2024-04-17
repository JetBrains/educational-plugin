package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import org.junit.Test

class HyperskillSortingBasedTaskOrderingTest : EduTestCase() {
  @Test
  fun `test initial ordering in sorting based tasks`() {
    val task = SortingTask()
    task.options = listOf("A", "B", "C")
    assertEquals(listOf(0, 1, 2), task.ordering.toList())
  }

  @Test
  fun `test get ordered options`() {
    val task = SortingTask()
    task.options = listOf("A", "B", "C")
    task.ordering = intArrayOf(1, 2, 0)
    assertEquals(listOf("B", "C", "A"), task.getOrderedOptions())
  }

  @Test
  fun `test swap options`() {
    val task = SortingTask()
    task.options = listOf("A", "B", "C")
    task.swapOptions(0, 2)
    assertEquals(listOf(2, 1, 0), task.ordering.toList())
    assertEquals(listOf("C", "B", "A"), task.getOrderedOptions())
    task.swapOptions(0, 1)
    assertEquals(listOf(1, 2, 0), task.ordering.toList())
    assertEquals(listOf("B", "C", "A"), task.getOrderedOptions())
  }

  @Test
  fun `test move up`() {
    val task = SortingTask()
    task.options = listOf("A", "B", "C")
    task.moveOptionUp(1)
    assertEquals(listOf(1, 0, 2), task.ordering.toList())
    assertEquals(listOf("B", "A", "C"), task.getOrderedOptions())
    task.moveOptionUp(2)
    assertEquals(listOf(1, 2, 0), task.ordering.toList())
    assertEquals(listOf("B", "C", "A"), task.getOrderedOptions())
  }

  @Test
  fun `test move down`() {
    val task = SortingTask()
    task.options = listOf("A", "B", "C")
    task.moveOptionDown(1)
    assertEquals(listOf(0, 2, 1), task.ordering.toList())
    assertEquals(listOf("A", "C", "B"), task.getOrderedOptions())
    task.moveOptionDown(0)
    assertEquals(listOf(2, 0, 1), task.ordering.toList())
    assertEquals(listOf("C", "A", "B"), task.getOrderedOptions())
  }

  @Test
  fun `test move up out of bounds`() {
    val task = SortingTask()
    task.options = listOf("A", "B", "C")
    assertThrows(Throwable::class.java, "There was an attempt to move the option up out of bounds") {
      task.moveOptionUp(0)
    }
  }

  @Test
  fun `test move down out of bounds`() {
    val task = SortingTask()
    task.options = listOf("A", "B", "C")
    assertThrows(Throwable::class.java, "There was an attempt to move the option down out of bounds") {
      task.moveOptionDown(2)
    }
  }

  @Test
  fun `test restore initial ordering`() {
    val task = SortingTask()
    task.options = listOf("A", "B", "C")
    task.ordering = intArrayOf(2, 1, 0)
    task.restoreInitialOrdering()
    assertEquals(listOf(0, 1, 2), task.ordering.toList())
  }
}