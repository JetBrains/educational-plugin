package com.jetbrains.edu.learning.format

import com.intellij.testFramework.fixtures.BasePlatformTestCase.assertEquals
import com.intellij.testFramework.fixtures.BasePlatformTestCase.assertTrue
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun doTestPlaceholderAndDependencyVisibility(task: Task, expectedPlaceholderVisibility: Boolean) {
  val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
  val placeholderDependency = answerPlaceholder.placeholderDependency

  assertEquals("Unexpected placeholder visibility", expectedPlaceholderVisibility, answerPlaceholder.isVisible)
  if (placeholderDependency != null) {
    assertTrue("Placeholder dependencies should be visible", placeholderDependency.isVisible)
  }
}