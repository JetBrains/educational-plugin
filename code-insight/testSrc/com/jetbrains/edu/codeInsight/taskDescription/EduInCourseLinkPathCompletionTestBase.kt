package com.jetbrains.edu.codeInsight.taskDescription

abstract class EduInCourseLinkPathCompletionTestBase : EduTaskDescriptionCompletionTestBase() {
  fun `test complete section`() = doTest("course://sect<caret>", "course://section1<caret>")
  fun `test complete lesson`() = doTest("course://less<caret>", "course://lesson1<caret>")
  fun `test complete task`() = doTest("course://lesson1/tas<caret>", "course://lesson1/task1<caret>")
  fun `test complete task file`() = doTest("course://lesson1/task1/Ta<caret>", "course://lesson1/task1/Task.txt<caret>")
  fun `test complete directory in task`() = doTest("course://lesson1/task1/fo<caret>", "course://lesson1/task1/foo/<caret>")
  fun `test don't suggest non task files`() = checkNoCompletion("course://lesson1/task1/task.ht<caret>")
  fun `test don't suggest test dir`() = checkNoCompletion("course://lesson1/task1/test<caret>")
  fun `test don't suggest test task file`() = checkNoCompletion("course://lesson1/task1/tests/te<caret>")
}
