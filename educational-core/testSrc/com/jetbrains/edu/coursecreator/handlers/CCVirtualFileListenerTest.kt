package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.`in`
import com.jetbrains.edu.learning.configurators.FakeGradleConfigurator
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener
import com.jetbrains.edu.learning.handlers.VirtualFileListenerTestBase
import com.jetbrains.edu.learning.notIn
import junit.framework.TestCase

class CCVirtualFileListenerTest : VirtualFileListenerTestBase() {

  override val courseMode: String = CCUtils.COURSE_MODE

  override fun createListener(project: Project): EduVirtualFileListener = CCVirtualFileListener(project)

  fun `test delete section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section()
      lesson()
    }
    val sectionVFile = findFile("section2")
    runWriteAction {
      sectionVFile.delete(this)
    }

    TestCase.assertEquals(2, course.items.size)
    TestCase.assertNull(course.getSection("section2"))
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
  }

  fun `test delete not empty section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson()
      }
      lesson()
    }
    val sectionVFile = findFile("section2")
    runWriteAction {
      sectionVFile.delete(this)
    }

    TestCase.assertEquals(2, course.items.size)
    TestCase.assertNull(course.getSection("section2"))
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
  }

  fun `test delete lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section()
      lesson()
    }
    val lesson1 = findFile("lesson1")
    runWriteAction {
      lesson1.delete(this)
    }

    TestCase.assertEquals(2, course.items.size)
    TestCase.assertNull(course.getLesson("lesson1"))
    TestCase.assertEquals(1, course.getSection("section2")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
  }


  fun `test delete lesson from section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson()
        lesson()
      }
      lesson()
    }
    val lesson1 = findFile("section2/lesson1")
    runWriteAction {
      lesson1.delete(this)
    }

    val section = course.getSection("section2")!!
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section.index)
    TestCase.assertEquals(3, course.getLesson("lesson2")!!.index)

    TestCase.assertNull(section.getLesson("lesson1"))
    TestCase.assertEquals(1, section.items.size)
    TestCase.assertEquals(1, section.getLesson("lesson2")!!.index)
  }

  fun `test delete task`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("tmp.txt")
        }
      }
    }
    val task1 = findFile("lesson1/task1")
    runWriteAction {
      task1.delete(this)
    }

    val lesson = course.getLesson("lesson1")
    TestCase.assertNull(lesson!!.getTask("task1"))
  }

  fun `test add task file`() {
    val filePath = "src/taskFile.txt"
    doAddFileTest(filePath) { task -> listOf(filePath `in` task) }
  }

  fun `test add test file`() {
    doAddFileTest("test/${FakeGradleConfigurator.TEST_FILE_NAME}") { task ->
      listOf(("test/${FakeGradleConfigurator.TEST_FILE_NAME}" `in` task).withAdditionalCheck {
        check(!it.isVisible) { "Test file should be invisible by default" }
      })
    }
  }

  fun `test add additional file`() {
    val fileName = "additionalFile.txt"
    doAddFileTest(fileName) { task -> listOf(fileName `in` task) }
  }

  fun `test remove task file`() {
    val filePath = "src/TaskFile.kt"
    doRemoveFileTest("lesson1/task1/$filePath") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(filePath notIn task)
    }
  }

  fun `test remove test file`() {
    val filePath = "test/${FakeGradleConfigurator.TEST_FILE_NAME}"
    doRemoveFileTest("lesson1/task1/$filePath") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(filePath notIn task)
    }
  }

  fun `test remove additional file`() {
    val fileName = "additionalFile.txt"
    doRemoveFileTest("lesson1/task1/$fileName") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(fileName notIn task)
    }
  }

  fun `test remove src folder`() {
    doRemoveFileTest("lesson1/task1/src/packageName") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "src/packageName/TaskFile2.kt" notIn task,
        "src/packageName/TaskFile3.kt" notIn task
      )
    }
  }

  fun `test remove test folder`() {
    doRemoveFileTest("lesson1/task1/test/packageName") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "test/packageName/Tests2.kt" notIn task,
        "test/packageName/Tests3.kt" notIn task
      )
    }
  }

  fun `test remove additional folder`() {
    doRemoveFileTest("lesson1/task1/additional_files") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "additional_files/additional_file2.txt" notIn task,
        "additional_files/additional_file2.txt" notIn task
      )
    }
  }

  fun `test rename task file`() {
    doRenameFileTest("lesson1/task1/src/packageName/Task1.kt", "Task3.kt") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "src/packageName/Task1.kt" notIn task,
        "src/packageName/Task3.kt" `in` task
      )
    }
  }

  fun `test rename directory with task files`() {
    doRenameFileTest("lesson1/task1/src/packageName", "packageName2") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "src/packageName/Task1.kt" notIn task,
        "src/packageName/Task2.kt" notIn task,
        "src/packageName2/Task1.kt" `in` task,
        "src/packageName2/Task2.kt" `in` task
      )
    }
  }

  fun `test rename test file`() {
    doRenameFileTest("lesson1/task1/test/packageName/Test1.kt", "Test3.kt") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "test/packageName/Test1.kt" notIn task,
        "test/packageName/Test3.kt" `in` task
      )
    }
  }

  fun `test rename directory with test files`() {
    doRenameFileTest("lesson1/task1/test/packageName", "packageName2") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "test/packageName/Test1.kt" notIn task,
        "test/packageName/Test2.kt" notIn task,
        "test/packageName2/Test1.kt" `in` task,
        "test/packageName2/Test2.kt" `in` task
      )
    }
  }

  fun `test rename additional file`() {
    doRenameFileTest("lesson1/task1/additional_files/additional_file1.txt", "additional_file3.txt") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "additional_files/additional_file1.txt" notIn task,
        "additional_files/additional_file3.txt" `in` task
      )
    }
  }

  fun `test rename directory with additional files`() {
    doRenameFileTest("lesson1/task1/additional_files", "additional_files2") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "additional_files/additional_file1.txt" notIn task,
        "additional_files/additional_file2.txt" notIn task,
        "additional_files2/additional_file1.txt" `in` task,
        "additional_files2/additional_file2.txt" `in` task
      )
    }
  }

  fun `test move task file`() = doMoveTest("lesson1/task1/src/Task1.kt", "lesson1/task1/src/foo") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "src/Task1.kt" notIn task,
      "src/foo/Task1.kt" `in` task
    )
  }

  fun `test move dir with task files`() = doMoveTest("lesson1/task1/src/foo", "lesson1/task1/src/bar") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "src/foo/Task2.kt" notIn task,
      "src/foo/Task3.kt" notIn task,
      "src/bar/foo/Task2.kt" `in` task,
      "src/bar/foo/Task3.kt" `in` task,
      "src/bar/Task4.kt" `in` task
    )
  }

  fun `test move test file`() = doMoveTest("lesson1/task1/test/Tests1.kt", "lesson1/task1/test/foo") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "test/Tests1.kt" notIn task,
      "test/foo/Tests1.kt" `in` task
    )
  }

  fun `test move dir with tests`() = doMoveTest("lesson1/task1/test/foo", "lesson1/task1/test/bar") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "test/foo/Tests2.kt" notIn task,
      "test/foo/Tests3.kt" notIn task,
      "test/bar/foo/Tests2.kt" `in` task,
      "test/bar/foo/Tests3.kt" `in` task,
      "test/bar/Tests4.kt" `in` task
    )
  }

  fun `test move additional file 1`() = doMoveTest("lesson1/task1/additional_file1.txt", "lesson1/task1/foo") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "additional_file1.txt" notIn task,
      "foo/additional_file1.txt" `in` task
    )
  }

  fun `test move additional file 2`() = doMoveTest("lesson1/task1/foo/additional_file2.txt", "lesson1/task1") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "foo/additional_file2.txt" notIn task,
      "additional_file2.txt" `in` task
    )
  }

  fun `test move dir with additional files`() = doMoveTest("lesson1/task1/foo", "lesson1/task1/bar") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "foo/additional_file2.txt" notIn task,
      "foo/additional_file3.txt" notIn task,
      "bar/foo/additional_file2.txt" `in` task,
      "bar/foo/additional_file3.txt" `in` task,
      "bar/additional_file4.txt" `in` task
    )
  }

  fun `test move additional file into test folder`() = doMoveTest("lesson1/task1/additional_file1.txt", "lesson1/task1/test") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "additional_file1.txt" notIn task,
      "test/additional_file1.txt" `in` task
    )
  }

  fun `test move test package into src folder`() = doMoveTest("lesson1/task1/test/bar", "lesson1/task1/src/foo") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "test/bar/Tests4.kt" notIn task,
      "src/foo/bar/Tests4.kt" `in` task,
      "src/foo/Task2.kt" `in` task,
      "src/foo/Task3.kt" `in` task
    )
  }

  fun `test move non course file as src file`() = doMoveTest("non_course_dir/non_course_file1.txt", "lesson1/task1/src") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf("src/non_course_file1.txt" `in` task)
  }

  fun `test move non course file as test file`() = doMoveTest("non_course_dir/non_course_file1.txt", "lesson1/task1/test") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(("test/non_course_file1.txt" `in` task).withAdditionalCheck {
      check(!it.isVisible) { "Test file should be invisible by default" }
    })
  }

  fun `test move non course file as additional file`() = doMoveTest("non_course_dir/non_course_file1.txt", "lesson1/task1") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf("non_course_file1.txt" `in` task)
  }

  fun `test move non course folder to src folder`() = doMoveTest("non_course_dir", "lesson1/task1/src") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "src/non_course_dir/non_course_file1.txt" `in` task,
      "src/non_course_dir/non_course_file2.txt" `in` task
    )
  }

  fun `test move non course folder to test folder`() = doMoveTest("non_course_dir", "lesson1/task1/test") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "test/non_course_dir/non_course_file1.txt" `in` task,
      "test/non_course_dir/non_course_file2.txt" `in` task
    )
  }

  fun `test move non course folder to task root folder`() = doMoveTest("non_course_dir", "lesson1/task1") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "non_course_dir/non_course_file1.txt" `in` task,
      "non_course_dir/non_course_file2.txt" `in` task
    )
  }
}
