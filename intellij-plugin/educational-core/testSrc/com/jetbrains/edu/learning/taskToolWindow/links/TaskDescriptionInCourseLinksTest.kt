package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.selectedVirtualFile
import org.junit.Test

class TaskDescriptionInCourseLinksTest : TaskDescriptionLinksTestBase() {

  @Test
  fun `test empty link`() = doTest("course://")

  @Test
  fun `test section link`() = doTest("course://section1")
  @Test
  fun `test non-existent section link`() = doTest("course://section2")

  @Test
  fun `test lesson link 1`() = doTest("course://lesson1")
  @Test
  fun `test lesson link 2`() = doTest("course://section/lesson4")
  @Test
  fun `test non-existent lesson link`() = doTest("course://lesson10")

  @Test
  fun `test task link 1`() = doTest("course://lesson1/task1", "lesson1/task1/TaskFile1.txt")
  @Test
  fun `test task link 2`() = doTest("course://section1/lesson4/task5", "section1/lesson4/task5/TaskFile8.txt")
  @Test
  fun `test non-existent task link`() = doTest("course://lesson1/task11")

  @Test
  fun `test link to current task of framework lesson`() =
    doTest("course://framework%20lesson%203/task3", "framework lesson 3/task/TaskFile6.txt")

  @Test
  fun `test link to non current task of framework lesson`() = doTest("course://framework%20lesson%203/task4")

  @Test
  fun `test link to task file 1`() = doTest("course://lesson1/task1/TaskFile2.txt", "lesson1/task1/TaskFile2.txt")
  @Test
  fun `test link to task file 2`() = doTest("course://lesson2/task2/Task%20File%205.txt", "lesson2/task2/Task File 5.txt")
  @Test
  fun `test link to task file 3`() = doTest("course://section1/lesson4/task5/TaskFile9.txt", "section1/lesson4/task5/TaskFile9.txt")
  @Test
  fun `test link to task file 4`() = doTest("course://section1/lesson 5/task 6/Task File 10.txt", "section1/lesson 5/task 6/Task File 10.txt")

  @Test
  fun `test link to non-existent task file`() = doTest("course://lesson2/task2/TaskFile20.txt")

  @Test
  fun `test link to task file in current task of framework lesson`() =
    doTest("course://framework%20lesson%203/task3/TaskFile6.txt", "framework lesson 3/task/TaskFile6.txt")

  @Test
  fun `test link to task file in current task of framework lesson in educator mode`() =
    doTest("course://framework%20lesson%203/task3/TaskFile6.txt", "framework lesson 3/task3/TaskFile6.txt", CourseMode.EDUCATOR)

  @Test
  fun `test link to task file in non current task of framework lesson`() = doTest("course://framework%20lesson%203/task4/TaskFile7.txt")

  @Test
  fun `test don't close opened files 1`() =
    doTest("course://lesson2/task2/TaskFile3.txt", "lesson2/task2/TaskFile3.txt", openedFile = "lesson1/task1/TaskFile1.txt")

  @Test
  fun `test don't close opened files 2`() =
    doTest("course://lesson2/task2", "lesson2/task2/TaskFile3.txt", openedFile = "lesson1/task1/TaskFile1.txt")

  private fun doTest(url: String, expectedPath: String? = null, courseMode: CourseMode = CourseMode.STUDENT, openedFile: String? = null) {
    createCourse(courseMode)
    if (openedFile != null) {
      val file = findFile(openedFile)
      myFixture.openFileInEditor(file)
    }

    openInCourseLink(url, expectedPath)
    if (openedFile != null) {
      val file = findFile(openedFile)
      val openFiles = FileEditorManager.getInstance(project).openFiles
      check(file in openFiles) {
        "$file should be opened"
      }
    }
  }

  private fun openInCourseLink(url: String, expectedPath: String?) {
    openLink(url)

    val selectedFile = project.selectedVirtualFile
    if (expectedPath == null) {
      check(selectedFile == null) { "unexpected $selectedFile is open" }
    }
    else {
      val expectedFile = findFile(expectedPath)
      assertEquals(expectedFile, selectedFile)
    }
  }

  private fun createCourse(courseMode: CourseMode) {
    courseWithFiles(courseMode = courseMode) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("TaskFile1.txt")
          taskFile("TaskFile2.txt")
        }
      }
      lesson("lesson2") {
        eduTask("task2") {
          taskFile("TaskFile3.txt")
          taskFile("TaskFile4.txt")
          taskFile("Task File 5.txt")
        }
      }
      frameworkLesson("framework lesson 3") {
        eduTask("task3") {
          taskFile("TaskFile6.txt")
        }
        eduTask("task4") {
          taskFile("TaskFile7.txt")
        }
      }
      section("section1") {
        lesson("lesson4") {
          eduTask("task5") {
            taskFile("TaskFile8.txt")
            taskFile("TaskFile9.txt")
          }
        }
        lesson("lesson 5") {
          eduTask("task 6") {
            taskFile("Task File 10.txt")
          }
        }
      }
    }
  }
}
