package com.jetbrains.edu.codeInsight.taskDescription

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.junit.Test

abstract class EduInCourseLinkPathCompletionTestBase(format: DescriptionFormat) : EduTaskDescriptionCompletionTestBase(format) {
  @Test
  fun `test complete section`() = doTest("course://sect<caret>", "course://section1<caret>")
  @Test
  fun `test complete lesson`() = doTest("course://less<caret>", "course://lesson1<caret>")
  @Test
  fun `test complete task`() = doTest("course://lesson1/tas<caret>", "course://lesson1/task1<caret>")
  @Test
  fun `test complete task file`() = doTest("course://lesson1/task1/Ta<caret>", "course://lesson1/task1/Task.txt<caret>")
  @Test
  fun `test complete directory in task`() = doTest("course://lesson1/task1/fo<caret>", "course://lesson1/task1/foo/<caret>")
  @Test
  fun `test don't suggest non task files`() = checkNoCompletion("course://lesson1/task1/task.ht<caret>")
  @Test
  fun `test don't suggest test dir`() = checkNoCompletion("course://lesson1/task1/test<caret>")
  @Test
  fun `test don't suggest test task file`() = checkNoCompletion("course://lesson1/task1/tests/te<caret>")
}
