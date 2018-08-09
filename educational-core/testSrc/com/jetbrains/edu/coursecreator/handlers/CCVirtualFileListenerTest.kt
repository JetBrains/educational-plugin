package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListenerTest.FileSetKind.*
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import junit.framework.TestCase

class CCVirtualFileListenerTest : EduTestCase() {

  private lateinit var listener: VirtualFileListener

  override fun setUp() {
    super.setUp()
    listener = CCVirtualFileListener(project)
    VirtualFileManager.getInstance().addVirtualFileListener(listener)
  }

  override fun tearDown() {
    super.tearDown()
    VirtualFileManager.getInstance().removeVirtualFileListener(listener)
  }

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
    val fileName = "taskFile.txt"
    doAddFileTest("src/$fileName") { task -> listOf(fileName to TASK_FILES `in` task) }
  }

  fun `test add test file`() {
    doAddFileTest("test/${FakeGradleConfigurator.TEST_FILE_NAME}") { task ->
      listOf(FakeGradleConfigurator.TEST_FILE_NAME to TEST_FILES `in` task)
    }
  }

  fun `test add additional file`() {
    val fileName = "additionalFile.txt"
    doAddFileTest(fileName) { task -> listOf(fileName to ADDITIONAL_FILES `in` task) }
  }

  fun `test remove task file`() {
    val fileName = "TaskFile.kt"
    doRemoveFileTest("lesson1/task1/src/$fileName") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(fileName to TASK_FILES notIn task)
    }
  }

  fun `test remove test file`() {
    val fileName = FakeGradleConfigurator.TEST_FILE_NAME
    doRemoveFileTest("lesson1/task1/test/$fileName") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(fileName to TEST_FILES notIn task)
    }
  }

  fun `test remove additional file`() {
    val fileName = "additionalFile.txt"
    doRemoveFileTest("lesson1/task1/$fileName") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(fileName to ADDITIONAL_FILES notIn task)
    }
  }

  fun `test rename task file`() {
    doRenameFileTest("lesson1/task1/src/packageName/Task1.kt", "Task3.kt") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "packageName/Task1.kt" to TASK_FILES notIn task,
        "packageName/Task3.kt" to TASK_FILES `in` task
      )
    }
  }

  fun `test rename directory with task files`() {
    doRenameFileTest("lesson1/task1/src/packageName", "packageName2") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "packageName/Task1.kt" to TASK_FILES notIn task,
        "packageName/Task2.kt" to TASK_FILES notIn task,
        "packageName2/Task1.kt" to TASK_FILES `in` task,
        "packageName2/Task2.kt" to TASK_FILES `in` task
      )
    }
  }

  fun `test rename test file`() {
    doRenameFileTest("lesson1/task1/test/packageName/Test1.kt", "Test3.kt") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "packageName/Test1.kt" to TEST_FILES notIn task,
        "packageName/Test3.kt" to TEST_FILES `in` task
      )
    }
  }

  fun `test rename directory with test files`() {
    doRenameFileTest("lesson1/task1/test/packageName", "packageName2") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "packageName/Test1.kt" to TEST_FILES notIn task,
        "packageName/Test2.kt" to TEST_FILES notIn task,
        "packageName2/Test1.kt" to TEST_FILES `in` task,
        "packageName2/Test2.kt" to TEST_FILES `in` task
      )
    }
  }

  fun `test rename additional file`() {
    doRenameFileTest("lesson1/task1/additional_files/additional_file1.txt", "additional_file3.txt") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "additional_files/additional_file1.txt" to ADDITIONAL_FILES notIn task,
        "additional_files/additional_file3.txt" to ADDITIONAL_FILES `in` task
      )
    }
  }

  fun `test rename directory with additional files`() {
    doRenameFileTest("lesson1/task1/additional_files", "additional_files2") { course ->
      val task = course.findTask("lesson1", "task1")
      listOf(
        "additional_files/additional_file1.txt" to ADDITIONAL_FILES notIn task,
        "additional_files/additional_file2.txt" to ADDITIONAL_FILES notIn task,
        "additional_files2/additional_file1.txt" to ADDITIONAL_FILES `in` task,
        "additional_files2/additional_file2.txt" to ADDITIONAL_FILES `in` task
      )
    }
  }

  private fun doAddFileTest(filePathInTask: String, checksProducer: (Task) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = FakeGradleBasedLanguage,
      settings = JdkProjectSettings.emptySettings()
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt")
        }
      }
    }

    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getTaskDir(project) ?: error("Failed to find directory of `${task.name}` task")

    GeneratorUtils.createChildFile(taskDir, filePathInTask, "")
    checksProducer(task).forEach(FileCheck::check)
  }

  private fun doRemoveFileTest(filePathInCourse: String, checksProducer: (Course) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = FakeGradleBasedLanguage,
      settings = JdkProjectSettings.emptySettings()
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("TaskFile.kt")
          additionalFile("additionalFile.txt")
          testFile(FakeGradleConfigurator.TEST_FILE_NAME)
        }
      }
      section("section1") {
        lesson("lesson2")
      }
    }

    val file = findFile(filePathInCourse)
    runWriteAction { file.delete(CCVirtualFileListenerTest::class.java) }
    checksProducer(course).forEach(FileCheck::check)
  }

  private fun doRenameFileTest(filePathInCourse: String, newName: String, checksProducer: (Course) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = FakeGradleBasedLanguage,
      settings = JdkProjectSettings.emptySettings()
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          dir("packageName") {
            taskFile("Task1.kt")
            taskFile("Task2.kt")
          }

          dir("additional_files") {
            additionalFile("additional_file1.txt")
            additionalFile("additional_file2.txt")
          }
          dir("packageName") {
            testFile("Test1.kt")
            testFile("Test2.kt")
          }
        }
      }
    }

    val file = findFile(filePathInCourse)
    val psiFileSystemItem = if (file.isDirectory) {
      PsiManager.getInstance(project).findDirectory(file) ?: error("Can't find psi directory for $file")
    } else {
      PsiManager.getInstance(project).findFile(file) ?: error("Can't find psi file for $file")
    }
    myFixture.renameElement(psiFileSystemItem, newName)
    val checks = checksProducer(course)
    checks.forEach(FileCheck::check)

    // at 173 platform version `UndoManager.undo(null)` doesn't revert result of `myFixture.renameElement`
    // so skip next checks
    // TODO: drop this condition when 173 becomes unsupported
    if (ApplicationInfo.getInstance().build < BuildNumber.fromString("181.0")) return
    val dialog = EduTestDialog()
    withTestDialog(dialog) {
      UndoManager.getInstance(project).undo(null)
    }
    checks.map(FileCheck::invert).forEach(FileCheck::check)
    withTestDialog(dialog) {
      UndoManager.getInstance(project).redo(null)
    }
    checks.forEach(FileCheck::check)
  }
  
  private enum class FileSetKind(val fileSetName: String) {
    TASK_FILES("task files"),
    TEST_FILES("test files"),
    ADDITIONAL_FILES("additional files");
    
    fun fileSet(task: Task): Map<String, Any> = when (this) {
      TASK_FILES -> task.taskFiles
      TEST_FILES -> task.testsText
      ADDITIONAL_FILES -> task.additionalFiles
    }
  }

  private data class FileCheck(
    val task: Task,
    val path: String,
    val kind: FileSetKind,
    val shouldContain: Boolean
  ) {
    fun invert(): FileCheck = copy(shouldContain = !shouldContain)
    fun check() {
      if (shouldContain) {
        check(path in kind.fileSet(task)) {
          "`$path` should be in ${kind.fileSetName} of `${task.name}` task"
        }
      } else {
        check(path !in kind.fileSet(task)) {
          "`$path` shouldn't be in ${kind.fileSetName} of `${task.name}` task"
        }
      }
    }
  }

  private infix fun Pair<String, FileSetKind>.`in`(task: Task): FileCheck = FileCheck(task, first, second, true)
  private infix fun Pair<String, FileSetKind>.notIn(task: Task): FileCheck = FileCheck(task, first, second, false)
}
