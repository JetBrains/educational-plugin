package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.`in`
import com.jetbrains.edu.learning.notIn
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import org.junit.Test

class CCIncludeIntoCourseTest : CCChangeFileOwnerTestBase(CCIncludeIntoArchive.ACTION_ID) {

  @Test
  fun `test include single file`() = doAvailableTest("b-excluded.txt") { course ->
    Pair(
      listOf("a-included.txt" `in` course),
      listOf("b-excluded.txt" `in` course)
    )
  }

  @Test
  fun `test include several files`() = doAvailableTest(
    "b-excluded.txt", "dir/y-excluded.txt"
  ) { course ->
    Pair(
      listOf(),
      listOf(
        "b-excluded.txt" `in` course,
        "dir/y-excluded.txt" `in` course
      )
    )
  }

  @Test
  fun `test include directory`() = doAvailableTest("dir") { course ->
    Pair(
      listOf(
        "b-excluded.txt" notIn course,
        "dir/x-included.txt" `in` course
      ),
      listOf(
        "dir/y-excluded.txt" `in` course,
      )
    )
  }

  @Test
  fun `test include file and directory`() = doAvailableTest("b-excluded.txt", "dir") { course ->
    Pair(
      listOf(
        "dir/x-included.txt" `in` course,
        "a-included.txt" `in` course
      ),
      listOf(
        "b-excluded.txt" `in` course,
        "dir/y-excluded.txt" `in` course
      )
    )
  }

  @Test
  fun `include directory with files that must be excluded`() = doAvailableTest("dir") { course ->
    Pair(
      listOf("dir/must_not_include.iml" notIn course, "dir/x-included.txt" `in` course),
      listOf("dir/y-excluded.txt" `in` course)
    )
  }

  @Test
  fun `test do not include file inside task`() = doUnavailableTest("lesson1/task1/taskFile1.txt")
  @Test
  fun `test do not include file inside and outside task`() = doUnavailableTest("lesson1/task1/taskFile1.txt", "b-excluded.txt")
  @Test
  fun `test do not include file and a directory`() = doUnavailableTest("lesson1/task1/taskFile1.txt", "dir")

  override fun createCourse() {
    courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = FakeGradleBasedLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          task.id = 1
          taskFile("taskFile1.txt")
          taskFile("taskFile2.txt")
        }
      }

      additionalFile("a-included.txt")
      additionalFile("dir/x-included.txt")
    }

    fileTree {
      file("b-excluded.txt")
      dir("dir") {
        file("y-excluded.txt")
        file("must_not_include.iml")
      }
    }.create(LightPlatformTestCase.getSourceRoot())
  }
}
