package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.configurators.FakeGradleConfigurator
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.getStudyItem
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener
import com.jetbrains.edu.learning.handlers.VirtualFileListenerTestBase
import com.jetbrains.edu.learning.`in`
import com.jetbrains.edu.learning.notIn
import org.junit.Test

class CCVirtualFileListenerTest : VirtualFileListenerTestBase() {

  override val courseMode: CourseMode = CourseMode.EDUCATOR

  override fun createListener(project: Project): EduVirtualFileListener = CCVirtualFileListener(project, testRootDisposable)

  @Test
  fun `test delete task`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    assertNull(lesson!!.getTask("task1"))
  }

  @Test
  fun `test add task file`() {
    val filePath = "src/taskFile.txt"
    doAddFileTest(filePath) { task -> listOf(filePath `in` task) }
  }

  @Test
  fun `test add test file`() {
    doAddFileTest("test/${FakeGradleConfigurator.TEST_FILE_NAME}") { task ->
      listOf(("test/${FakeGradleConfigurator.TEST_FILE_NAME}" `in` task).withAdditionalCheck {
        check(!it.isVisible) { "Test file should be invisible by default" }
      })
    }
  }

  @Test
  fun `test add additional file`() {
    val fileName = "additionalFile.txt"
    doAddFileTest(fileName) { task -> listOf(fileName `in` task) }
  }

  @Test
  fun `test add custom run configuration file`() {
    val filePath = "runConfigurations/CustomRun.run.xml"
    doAddFileTest(filePath, "<component name=\"ProjectRunConfigurationManager\"></component>") { task ->
      listOf((filePath `in` task).withAdditionalCheck {
        check(!it.isVisible) { "Custom run configuration file should be invisible by default" }
      })
    }
  }

  @Test
  fun `test remove task file`() {
    val filePath = "src/TaskFile.kt"
    doRemoveFileTest("lesson1/task1/$filePath") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(filePath notIn task)
    }
  }

  @Test
  fun `test remove test file`() {
    val filePath = "test/${FakeGradleConfigurator.TEST_FILE_NAME}"
    doRemoveFileTest("lesson1/task1/$filePath") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(filePath notIn task)
    }
  }

  @Test
  fun `test remove additional file`() {
    val fileName = "additionalFile.txt"
    doRemoveFileTest("lesson1/task1/$fileName") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(fileName notIn task)
    }
  }

  @Test
  fun `test remove src folder`() {
    doRemoveFileTest("lesson1/task1/src/packageName") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "src/packageName/TaskFile2.kt" notIn task,
        "src/packageName/TaskFile3.kt" notIn task
      )
    }
  }

  @Test
  fun `test remove test folder`() {
    doRemoveFileTest("lesson1/task1/test/packageName") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "test/packageName/Tests2.kt" notIn task,
        "test/packageName/Tests3.kt" notIn task
      )
    }
  }

  @Test
  fun `test remove additional folder`() {
    doRemoveFileTest("lesson1/task1/additional_files") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "additional_files/additional_file2.txt" notIn task,
        "additional_files/additional_file2.txt" notIn task,
        "additional_files.txt" `in` task
      )
    }
  }

  @Test
  fun `test rename task file`() {
    doRenameFileTest("lesson1/task1/src/packageName/Task1.kt", "Task3.kt") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "src/packageName/Task1.kt" notIn task,
        "src/packageName/Task3.kt" `in` task
      )
    }
  }

  @Test
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

  @Test
  fun `test rename test file`() {
    doRenameFileTest("lesson1/task1/test/packageName/Test1.kt", "Test3.kt") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "test/packageName/Test1.kt" notIn task,
        "test/packageName/Test3.kt" `in` task
      )
    }
  }

  @Test
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

  @Test
  fun `test rename additional file`() {
    doRenameFileTest("lesson1/task1/additional_files/additional_file1.txt", "additional_file3.txt") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "additional_files/additional_file1.txt" notIn task,
        "additional_files/additional_file3.txt" `in` task
      )
    }
  }

  @Test
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

  @Test
  fun `test move task file`() = doMoveTest("lesson1/task1/src/Task1.kt", "lesson1/task1/src/foo") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "src/Task1.kt" notIn task,
      "src/foo/Task1.kt" `in` task
    )
  }

  @Test
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

  @Test
  fun `test move test file`() = doMoveTest("lesson1/task1/test/Tests1.kt", "lesson1/task1/test/foo") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "test/Tests1.kt" notIn task,
      "test/foo/Tests1.kt" `in` task
    )
  }

  @Test
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

  @Test
  fun `test move additional file 1`() = doMoveTest("lesson1/task1/additional_file1.txt", "lesson1/task1/foo") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "additional_file1.txt" notIn task,
      "foo/additional_file1.txt" `in` task
    )
  }

  @Test
  fun `test move additional file 2`() = doMoveTest("lesson1/task1/foo/additional_file2.txt", "lesson1/task1") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "foo/additional_file2.txt" notIn task,
      "additional_file2.txt" `in` task
    )
  }

  @Test
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

  @Test
  fun `test move additional file into test folder`() = doMoveTest("lesson1/task1/additional_file1.txt", "lesson1/task1/test") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "additional_file1.txt" notIn task,
      "test/additional_file1.txt" `in` task
    )
  }

  @Test
  fun `test move test package into src folder`() = doMoveTest("lesson1/task1/test/bar", "lesson1/task1/src/foo") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "test/bar/Tests4.kt" notIn task,
      "src/foo/bar/Tests4.kt" `in` task,
      "src/foo/Task2.kt" `in` task,
      "src/foo/Task3.kt" `in` task
    )
  }

  @Test
  fun `test move non course file as src file`() = doMoveTest("non_course_dir/non_course_file1.txt", "lesson1/task1/src") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf("src/non_course_file1.txt" `in` task)
  }

  @Test
  fun `test move non course file as test file`() = doMoveTest("non_course_dir/non_course_file1.txt", "lesson1/task1/test") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(("test/non_course_file1.txt" `in` task).withAdditionalCheck {
      check(!it.isVisible) { "Test file should be invisible by default" }
    })
  }

  @Test
  fun `test move non course file as additional file`() = doMoveTest("non_course_dir/non_course_file1.txt", "lesson1/task1") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf("non_course_file1.txt" `in` task)
  }

  @Test
  fun `test move non course folder to src folder`() = doMoveTest("non_course_dir", "lesson1/task1/src") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "src/non_course_dir/non_course_file1.txt" `in` task,
      "src/non_course_dir/non_course_file2.txt" `in` task
    )
  }

  @Test
  fun `test move non course folder to test folder`() = doMoveTest("non_course_dir", "lesson1/task1/test") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "test/non_course_dir/non_course_file1.txt" `in` task,
      "test/non_course_dir/non_course_file2.txt" `in` task
    )
  }

  @Test
  fun `test move non course folder to task root folder`() = doMoveTest("non_course_dir", "lesson1/task1") { course ->
    val task = course.findTask("lesson1", "task1")
    listOf(
      "non_course_dir/non_course_file1.txt" `in` task,
      "non_course_dir/non_course_file2.txt" `in` task
    )
  }

  @Test
  fun `test copy task between lessons EDU-2796`() =
    doCopyFileTest("lesson1/task1", "lesson2", "task1copy") { course ->
      val task1copy = course.findTask("lesson2", "task1copy")

      listOf(
        "src/Task1.kt" `in` task1copy,
        "src/foo/Task2.kt" `in` task1copy,
        "src/foo/Task3.kt" `in` task1copy,
        "src/bar/Task4.kt" `in` task1copy
      )
    }

  @Test
  fun `test copy lesson from course to inside a sections`() =
    doCopyFileTest("lesson1", "section1", "lesson1copy") { course ->
      val task3 = course.getSection("section1")?.getLesson("lesson1copy")?.getTask("task3") ?: error("there is no task3")

      listOf(
        "src/Task1.kt" `in` task3,
        "src/foo/Task2.kt" `in` task3,
        "src/foo/Task3.kt" `in` task3,
        "src/bar/Task4.kt" `in` task3
      )
    }

  @Test
  fun `test copy lessons between sections`() =
    doCopyFileTest("section1/lesson1", "section2", "lesson1copy") { course ->
      val lesson1copy = course.getSection("section2")?.getLesson("lesson1copy") ?: error("there is no lesson1copy")
      val task1 = lesson1copy.getTask("task1") ?: error("there is no task1 in the lesson1copy")

      listOf(
        "src/Task1.kt" `in` task1,
        "src/foo/Task2.kt" `in` task1,
        "additional_file1.txt" `in` task1,
        "foo/additional_file2.txt" `in` task1
      )
    }

  @Test
  fun `test copy lesson from course to section`() =
    doCopyFileTest("lesson1", "section1", "lesson1copy") { course ->
      val lesson1copy = course.getSection("section1")?.getLesson("lesson1copy") ?: error("there is no lesson1copy")
      val task1 = lesson1copy.getTask("task1") ?: error("there is no task1 in the lesson1copy")

      listOf(
        "src/Task1.kt" `in` task1,
        "src/foo/Task2.kt" `in` task1,
        "additional_file1.txt" `in` task1,
        "foo/additional_file2.txt" `in` task1
      )
    }

  @Test
  fun `test copy lesson from section to course`() =
    doCopyFileTest("section1/lesson1", "", "lesson1copy") { course ->
      val lesson1copy = course.getLesson("lesson1copy") ?: error("there is no lesson1copy")
      val task1 = lesson1copy.getTask("task1") ?: error("there is no task1 in the lesson1copy")

      listOf(
        "src/Task1.kt" `in` task1,
        "src/foo/Task2.kt" `in` task1,
        "additional_file1.txt" `in` task1,
        "foo/additional_file2.txt" `in` task1
      )
    }

  @Test
  fun `test copy sections`() =
    doCopyFileTest("section1", "", "section1copy") { course ->
      val lesson1 = course.getSection("section1copy")?.getLesson("lesson1") ?: error("there is no lesson1")
      val task1 = lesson1.getTask("task1") ?: error("there is no task1 in the lesson1copy")

      listOf(
        "src/Task1.kt" `in` task1,
        "src/foo/Task2.kt" `in` task1,
        "additional_file1.txt" `in` task1,
        "foo/additional_file2.txt" `in` task1
      )
    }

  @Test
  fun `test copy pasted files are included into yaml EDU-5191`() {
    doCopyFileTest("lesson1/task1/additional_file1.txt", "lesson1/task1", "additional_file1copy.txt") { course ->
      val task1 = course.findTask("lesson1", "task1")

      listOf(
        "additional_file1.txt" `in` task1,
        "additional_file1copy.txt" `in` task1
      )
    }
  }

  @Test
  fun `test files copied from the other task are included into yaml`() {
    doCopyFileTest("lesson1/task1/foo", "section1/lesson2/task2", "foo-copy") { course ->
      val task = course.getSection("section1")?.getLesson("lesson2")?.getTask("task2") ?: error("there is no lesson1")

      listOf(
        "additional_file1.txt" `in` task,
        "foo-copy/additional_file2.txt" `in` task,
        "foo-copy/additional_file3.txt" `in` task
      )
    }
  }

  private fun doTestNumberOfItemsDidNotChange(filePathInCourse: String, newParentPath: String, copyName: String? = null) {
    createCourseForCopyTests()

    val fileToCopy = findFile(filePathInCourse)
    val newParentFile = findFile(newParentPath)

    val parentItemBefore = newParentFile.getStudyItem(project)
    val itemsListBefore = (parentItemBefore as? ItemContainer)?.items?.toList() ?: listOf()

    runWriteAction {
      copy(CCVirtualFileListenerTest::class.java, fileToCopy, newParentFile, copyName ?: fileToCopy.name)
    }

    val parentItemAfter = newParentFile.getStudyItem(project)
    val itemsListAfter = (parentItemAfter as? ItemContainer)?.items?.toList() ?: listOf()

    assertEquals(parentItemBefore?.itemType, parentItemAfter?.itemType)
    assertEquals(itemsListBefore.map {it.itemType}, itemsListAfter.map {it.itemType})
  }

  @Test
  fun `test number of items do not change when copy task inside task`() =
    doTestNumberOfItemsDidNotChange("lesson1/task1", "lesson1/task2")

  @Test
  fun `test number of items do not change when copy lesson inside task`() =
    doTestNumberOfItemsDidNotChange("lesson1", "lesson2/task2")

  @Test
  fun `test number of items do not change when copy section inside task`() =
    doTestNumberOfItemsDidNotChange("section1", "lesson1/task2")

  @Test
  fun `test number of items do not change when copy lesson inside lesson`() =
    doTestNumberOfItemsDidNotChange("lesson1", "lesson2")

  @Test
  fun `test number of items do not change when copy section inside lesson`() =
    doTestNumberOfItemsDidNotChange("section1", "lesson1")

  @Test
  fun `test number of items do not change when copy section inside lesson (2)`() =
    doTestNumberOfItemsDidNotChange("section1", "section2/lesson1")

  @Test
  fun `test number of items do not change when copy task inside section`() =
    doTestNumberOfItemsDidNotChange("lesson1/task1", "section1")

  @Test
  fun `test number of items do not change when copy section inside section`() =
    doTestNumberOfItemsDidNotChange("section1", "section2")

  @Test
  fun `user created file is added to additional files`() =
    doTestAdditionalFilesAfterFSActions(emptyList(), listOf("file.txt")) {
      createFile("file.txt")
    }

  @Test
  fun `user created directory is not added to additional files`() =
    doTestAdditionalFilesAfterFSActions(listOf("file.txt"), listOf("file.txt")) {
      createDirectory("dir")
      createDirectory("dir/dir")
    }

  @Test
  fun `user created file in a subfolder is added to additional files`() =
    doTestAdditionalFilesAfterFSActions(emptyList(), listOf("folder/subfolder/file.txt")) {
      createFile("folder/subfolder/file.txt")
    }

  @Test
  fun `user created file is not added to additional files if it is excluded by configurator`() =
    doTestAdditionalFilesAfterFSActions(emptyList(), emptyList()) {
      createFile(".dot-file")
      createFile("folder/subfolder/.file")
    }

  @Test
  fun `delete additional file in a course folder`() =
    doTestAdditionalFilesAfterFSActions(listOf("1.txt"), emptyList()) {
      deleteFile("1.txt")
    }

  @Test
  fun `delete additional file with other files of the same name`() =
    doTestAdditionalFilesAfterFSActions(listOf("1.txt", "a/1.txt", "lesson1/1.txt"), listOf("1.txt", "lesson1/1.txt")) {
      deleteFile("a/1.txt")
    }

  @Test
  fun `delete additional folder`() =
    doTestAdditionalFilesAfterFSActions(listOf("a.txt", "a/1.txt", "a/2.txt", "aa/1.txt"), listOf("a.txt", "aa/1.txt")) {
      deleteFile("a")
    }
}
