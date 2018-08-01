package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.FakeGradleConfigurator
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
    doAddFileTest("src/$fileName") { task ->
      check(fileName in task.taskFiles) {
        "Expected `$fileName` in task files of `${task.name}` task"
      }
    }
  }

  fun `test add test file`() {
    doAddFileTest("test/${FakeGradleConfigurator.TEST_FILE_NAME}") { task ->
      check(FakeGradleConfigurator.TEST_FILE_NAME in task.testsText) {
        "Expected `${FakeGradleConfigurator.TEST_FILE_NAME}` in test files of `${task.name}` task"
      }
    }
  }

  fun `test add additional file`() {
    val fileName = "additionalFile.txt"
    doAddFileTest(fileName) { task ->
      check(fileName in task.additionalFiles) {
        "Can't find `$fileName` in additional files of `${task.name}` task"
      }
    }
  }

  fun `test remove task file`() {
    val fileName = "TaskFile.kt"
    doRemoveFileTest("lesson1/task1/src/$fileName") { course ->
      val task = course.findTask("lesson1", "task1")
      check(fileName !in task.taskFiles) {
        "$fileName shouldn't be in task files of `${task.name}` task"
      }
    }
  }

  fun `test remove test file`() {
    val fileName = FakeGradleConfigurator.TEST_FILE_NAME
    doRemoveFileTest("lesson1/task1/test/$fileName") { course ->
      val task = course.findTask("lesson1", "task1")
      check(fileName !in task.testsText) {
        "$fileName shouldn't be in test files of `${task.name}` task"
      }
    }
  }

  fun `test remove additional file`() {
    val fileName = "additionalFile.txt"
    doRemoveFileTest("lesson1/task1/$fileName") { course ->
      val task = course.findTask("lesson1", "task1")
      check(fileName !in task.taskFiles) {
        "$fileName shouldn't be in additional files of `${task.name}` task"
      }
    }
  }

  private fun doAddFileTest(filePathInTask: String, check: (Task) -> Unit) {
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
    check(task)
  }

  private fun doRemoveFileTest(filePathInCourse: String, check: (Course) -> Unit) {
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

    check(course)
  }
}
