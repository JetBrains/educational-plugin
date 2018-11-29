package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.`in`
import com.jetbrains.edu.coursecreator.notIn
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.gradle.JdkProjectSettings

class CCExcludeFromTaskTest : CCChangeFileOwnerTestBase(CCExcludeFromTask()) {

  fun `test exclude single src file`() = doAvailableTest("lesson1/task1/src/folder1/taskFile1.txt") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("src/folder1/taskFile2.txt" `in` task),
      listOf("src/folder1/taskFile1.txt" notIn task)
    )
  }

  fun `test exclude several src files`() = doAvailableTest(
    "lesson1/task1/src/folder1/taskFile1.txt",
    "lesson1/task1/src/folder1/taskFile2.txt"
  ) { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf(),
      listOf(
        "src/folder1/taskFile1.txt" notIn task,
        "src/folder1/taskFile2.txt" notIn task
      )
    )
  }

  fun `test exclude src folder`() = doAvailableTest("lesson1/task1/src/folder1") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf(),
      listOf(
        "src/folder1/taskFile1.txt" notIn task,
        "src/folder1/taskFile2.txt" notIn task
      )
    )
  }

  fun `test exclude single test file`() = doAvailableTest("lesson1/task1/test/folder2/testFile1.txt") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("test/folder2/testFile2.txt" `in` task),
      listOf("test/folder2/testFile1.txt" notIn task)
    )
  }

  fun `test exclude several test files`() = doAvailableTest(
    "lesson1/task1/test/folder2/testFile1.txt",
    "lesson1/task1/test/folder2/testFile2.txt"
  ) { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf(),
      listOf(
        "test/folder2/testFile1.txt" notIn task,
        "test/folder2/testFile2.txt" notIn task
      )
    )
  }

  fun `test exclude test folder`() = doAvailableTest("lesson1/task1/test/folder2") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf(),
      listOf(
        "test/folder2/testFile1.txt" notIn task,
        "test/folder2/testFile2.txt" notIn task
      )
    )
  }

  fun `test exclude single additional file`() = doAvailableTest("lesson1/task1/folder3/additionalFile1.txt") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf("folder3/additionalFile2.txt" `in` task),
      listOf("folder3/additionalFile1.txt" notIn task)
    )
  }

  fun `test exclude several additional files`() = doAvailableTest(
    "lesson1/task1/folder3/additionalFile1.txt",
    "lesson1/task1/folder3/additionalFile2.txt"
  ) { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf(),
      listOf(
        "folder3/additionalFile1.txt" notIn task,
        "folder3/additionalFile2.txt" notIn task
      )
    )
  }

  fun `test exclude additional folder`() = doAvailableTest("lesson1/task1/folder3") { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf(),
      listOf(
        "folder3/additionalFile1.txt" notIn task,
        "folder3/additionalFile2.txt" notIn task
      )
    )
  }

  fun `test exclude different files`() = doAvailableTest(
    "lesson1/task1/src/folder1/taskFile1.txt",
    "lesson1/task1/test/folder2/testFile1.txt",
    "lesson1/task1/folder3/additionalFile1.txt"
  ) { course ->
    val task = course.findTask("lesson1", "task1")
    Pair(
      listOf(
        "src/folder1/taskFile2.txt" `in` task,
        "test/folder2/testFile2.txt" `in` task,
        "folder3/additionalFile2.txt" `in` task
      ),
      listOf(
        "src/folder1/taskFile1.txt" notIn task,
        "test/folder2/testFile1.txt" notIn task,
        "folder3/additionalFile1.txt" notIn task
      )
    )
  }

  fun `test do not exclude file not from task`() = doUnavailableTest("lesson1/task1/excluded_file1.txt")
  fun `test do not exclude file outside of task dir`() = doUnavailableTest("excluded_file2.txt")

  override fun createCourse() {
    courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = FakeGradleBasedLanguage,
      settings = JdkProjectSettings.emptySettings()
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          task.stepId = 1
          dir("src/folder1") {
            taskFile("taskFile1.txt")
            taskFile("taskFile2.txt")
          }
          dir("test/folder2") {
            taskFile("testFile1.txt")
            taskFile("testFile2.txt")
          }
          dir("folder3") {
            taskFile("additionalFile1.txt")
            taskFile("additionalFile2.txt")
          }
        }
      }
    }

    fileTree {
      dir("lesson1/task1") {
        file("excluded_file1.txt")
      }
      file("excluded_file2.txt")
    }.create(LightPlatformTestCase.getSourceRoot())
  }
}
