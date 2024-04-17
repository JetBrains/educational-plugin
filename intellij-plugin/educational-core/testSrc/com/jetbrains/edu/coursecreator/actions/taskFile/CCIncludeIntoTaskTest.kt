package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.`in`
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class CCIncludeIntoTaskTest : CCChangeFileOwnerTestBase(CCIncludeIntoTask.ACTION_ID) {

  @Test
  fun `test include single src file`() = doAvailableTest("lesson1/task1/src/excluded_folder1/excluded_file1.txt") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("src/taskFile.txt" `in` task),
      listOf("src/excluded_folder1/excluded_file1.txt" `in` task)
    )
  }

  @Test
  fun `test include several src files`() = doAvailableTest(
    "lesson1/task1/src/excluded_folder1/excluded_file1.txt",
    "lesson1/task1/src/excluded_folder1/excluded_file2.txt"
  ) { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("src/taskFile.txt" `in` task),
      listOf(
        "src/excluded_folder1/excluded_file1.txt" `in` task,
        "src/excluded_folder1/excluded_file2.txt" `in` task
      )
    )
  }

  @Test
  fun `test include src folder`() = doAvailableTest("lesson1/task1/src/excluded_folder1") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("src/taskFile.txt" `in` task),
      listOf(
        "src/excluded_folder1/excluded_file1.txt" `in` task,
        "src/excluded_folder1/excluded_file2.txt" `in` task
      )
    )
  }

  @Test
  fun `test include single test file`() = doAvailableTest("lesson1/task1/test/excluded_folder2/excluded_file3.txt") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("test/testFile.txt" `in` task),
      listOf("test/excluded_folder2/excluded_file3.txt" `in` task)
    )
  }

  @Test
  fun `test include two test files`() = doAvailableTest(
    "lesson1/task1/test/excluded_folder2/excluded_file3.txt",
    "lesson1/task1/test/excluded_folder2/excluded_file4.txt"
  ) { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("test/testFile.txt" `in` task),
      listOf(
        "test/excluded_folder2/excluded_file3.txt" `in` task,
        "test/excluded_folder2/excluded_file4.txt" `in` task
      )
    )
  }

  @Test
  fun `test include test folder`() = doAvailableTest("lesson1/task1/test/excluded_folder2") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("test/testFile.txt" `in` task),
      listOf(
        "test/excluded_folder2/excluded_file3.txt" `in` task,
        "test/excluded_folder2/excluded_file4.txt" `in` task
      )
    )
  }

  @Test
  fun `test include single additional file`() = doAvailableTest("lesson1/task1/excluded_folder3/excluded_file5.txt") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("additionalFile.txt" `in` task),
      listOf("excluded_folder3/excluded_file5.txt" `in` task)
    )
  }

  @Test
  fun `test include two additional files`() = doAvailableTest(
    "lesson1/task1/excluded_folder3/excluded_file5.txt",
    "lesson1/task1/excluded_folder3/excluded_file6.txt"
  ) { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("additionalFile.txt" `in` task),
      listOf(
        "excluded_folder3/excluded_file5.txt" `in` task,
        "excluded_folder3/excluded_file6.txt" `in` task
      )
    )
  }

  @Test
  fun `test include additional folder`() = doAvailableTest("lesson1/task1/excluded_folder3") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("additionalFile.txt" `in` task),
      listOf(
        "excluded_folder3/excluded_file5.txt" `in` task,
        "excluded_folder3/excluded_file6.txt" `in` task
      )
    )
  }

  @Test
  fun `test include different files`() = doAvailableTest(
    "lesson1/task1/src/excluded_folder1/excluded_file1.txt",
    "lesson1/task1/test/excluded_folder2/excluded_file3.txt",
    "lesson1/task1/excluded_folder3/excluded_file5.txt"
  ) { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf(
        "src/taskFile.txt" `in` task,
        "test/testFile.txt" `in` task,
        "additionalFile.txt" `in` task
      ),
      listOf(
        "src/excluded_folder1/excluded_file1.txt" `in` task,
        "test/excluded_folder2/excluded_file3.txt" `in` task,
        "excluded_folder3/excluded_file5.txt" `in` task
      )
    )
  }

  @Test
  fun `test do not include existing task file`() = doUnavailableTest("lesson1/task1/src/taskFile.txt")
  @Test
  fun `test do not include existing test file`() = doUnavailableTest("lesson1/task1/test/testFile.txt")
  @Test
  fun `test do not include existing additional file`() = doUnavailableTest("lesson1/task1/additionalFile.txt")
  @Test
  fun `test do not include file outside of task dir`() = doUnavailableTest("excluded_folder4/excluded_file7.txt")
  @Test
  fun `test do not include folder outside of task dir`() = doUnavailableTest("excluded_folder4")

  override fun createCourse() {
    courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = FakeGradleBasedLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          task.id = 1
          taskFile("src/taskFile.txt")
          taskFile("test/testFile.txt")
          taskFile("additionalFile.txt")
        }
      }
    }

    fileTree {
      dir("lesson1/task1") {
        dir("src/excluded_folder1") {
          file("excluded_file1.txt")
          file("excluded_file2.txt")
        }
        dir("test/excluded_folder2") {
          file("excluded_file3.txt")
          file("excluded_file4.txt")
        }
        dir("excluded_folder3") {
          file("excluded_file5.txt")
          file("excluded_file6.txt")
        }
      }
      dir("excluded_folder4") {
        file("excluded_file7.txt")
        file("excluded_file8.txt")
      }
    }.create(LightPlatformTestCase.getSourceRoot())
  }
}
