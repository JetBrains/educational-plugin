package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.`in`
import com.jetbrains.edu.learning.notIn
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import org.junit.Test

class CCExcludeFromCourseTest : CCChangeFileOwnerTestBase(CCExcludeFromArchive.ACTION_ID) {

  @Test
  fun `test exclude single file`() = doAvailableTest("a-included.txt") { course ->
    Pair(
      listOf("b-excluded.txt" notIn course),
      listOf("a-included.txt" notIn course)
    )
  }

  @Test
  fun `test exclude several files`() = doAvailableTest(
    "a-included.txt", "dir/x-included.txt"
  ) { course ->
    Pair(
      listOf(),
      listOf(
        "a-included.txt" notIn course,
        "dir/x-included.txt" notIn course
      )
    )
  }

  @Test
  fun `test exclude folder`() = doAvailableTest("dir") { course ->
    Pair(
      listOf(
        "a-included.txt" `in` course,
        "dir/y-excluded.txt" notIn course
      ),
      listOf(
        "dir/x-included.txt" notIn course,
      )
    )
  }

  @Test
  fun `test exclude file and folder`() = doAvailableTest("a-included.txt", "dir") { course ->
    Pair(
      listOf(
        "dir/y-excluded.txt" notIn course,
        "a-excluded.txt" notIn course
      ),
      listOf(
        "a-included.txt" notIn course,
        "dir/x-included.txt" notIn course
      )
    )
  }

  @Test
  fun `test do not exclude file inside task`() = doUnavailableTest("lesson1/task1/taskFile1.txt")
  @Test
  fun `test do not exclude file inside and outside task`() = doUnavailableTest("lesson1/task1/taskFile1.txt", "a-included.txt")
  @Test
  fun `test do not exclude file and a folder`() = doUnavailableTest("lesson1/task1/taskFile1.txt", "dir")
  @Test
  fun `test do not exclude already excluded file`() = doUnavailableTest("b-excluded.txt")

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
      }
    }.create(LightPlatformTestCase.getSourceRoot())
  }
}
