package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.handlers.CCDescriptionFileRenameHandler
import com.jetbrains.edu.coursecreator.handlers.CCLessonRenameHandler
import com.jetbrains.edu.coursecreator.handlers.CCSectionRenameHandler
import com.jetbrains.edu.coursecreator.handlers.CCTaskRenameHandler
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import junit.framework.TestCase

class CCRenameTest : CCActionTestCase() {

  fun `test rename section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    Messages.setTestInputDialog { "section2" }
    val sourceVFile = LightPlatformTestCase.getSourceRoot().findChild("section1")

    val dataContext = dataContext(sourceVFile!!)
    val renameHandler = CCSectionRenameHandler()
    TestCase.assertNotNull(renameHandler)
    renameHandler.invoke(project, null, null, dataContext)
    TestCase.assertEquals(1, course.items.size)
    TestCase.assertNull(course.getSection("section1"))
    TestCase.assertNotNull(course.getSection("section2"))
  }

  fun `test rename lesson in section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    Messages.setTestInputDialog { "lesson2" }
    val sourceVFile = VfsUtil.findRelativeFile(LightPlatformTestCase.getSourceRoot(), "section1", "lesson1")

    val dataContext = dataContext(sourceVFile!!)
    val renameHandler = CCLessonRenameHandler()
    TestCase.assertNotNull(renameHandler)
    renameHandler.invoke(project, null, null, dataContext)
    TestCase.assertEquals(1, course.items.size)
    val section = course.getSection("section1")
    TestCase.assertNotNull(section)
    TestCase.assertNotNull(section!!.getLesson("lesson2"))
    TestCase.assertNull(section.getLesson("lesson1"))
  }

  fun `test rename lesson in course`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    Messages.setTestInputDialog { "lesson2" }
    val sourceVFile = VfsUtil.findRelativeFile(LightPlatformTestCase.getSourceRoot(), "lesson1")

    val dataContext = dataContext(sourceVFile!!)
    val renameHandler = CCLessonRenameHandler()
    TestCase.assertNotNull(renameHandler)
    renameHandler.invoke(project, null, null, dataContext)
    TestCase.assertEquals(1, course.items.size)
    TestCase.assertNotNull(course.getLesson("lesson2"))
    TestCase.assertNull(course.getLesson("lesson1"))
  }

  fun `test rename task`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    Messages.setTestInputDialog { "task2" }
    val sourceVFile = VfsUtil.findRelativeFile(LightPlatformTestCase.getSourceRoot(), "lesson1", "task1")

    val dataContext = dataContext(sourceVFile!!)
    val renameHandler = CCTaskRenameHandler()
    TestCase.assertNotNull(renameHandler)
    renameHandler.invoke(project, null, null, dataContext)
    TestCase.assertEquals(1, course.items.size)
    val lesson = course.getLesson("lesson1")
    TestCase.assertNotNull(lesson)
    TestCase.assertNotNull(lesson!!.getTask("task2"))
    TestCase.assertNull(lesson.getTask("task1"))
  }

  fun `test rename task file`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    val sourceVFile = VfsUtil.findRelativeFile(LightPlatformTestCase.getSourceRoot(), "lesson1", "task1", "taskFile1.txt")
    val psiFile = PsiManager.getInstance(project).findFile(sourceVFile!!)
    myFixture.renameElement(psiFile!!, "taskFile2.txt")
    TestCase.assertEquals(1, course.items.size)
    val lesson = course.getLesson("lesson1")
    TestCase.assertNotNull(lesson)
    val task = lesson!!.getTask("task1")!!
    TestCase.assertNull(task.getTaskFile("taskFile1.txt"))
    TestCase.assertNotNull(task.getTaskFile("taskFile2.txt"))
  }

  fun `test rename task description file`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    Messages.setTestInputDialog(testInputDialog(EduNames.TASK_MD))
    val dataContext = dataContext(findDescriptionFile(EduNames.TASK_HTML) ?: error("Can't find `${EduNames.TASK_HTML}`"))
    val renameHandler = CCDescriptionFileRenameHandler()
    renameHandler.invoke(project, null, null, dataContext)

    assertEquals(DescriptionFormat.MD, course.lessons[0].taskList[0].descriptionFormat)
    assertNull(findDescriptionFile(EduNames.TASK_HTML))
    assertNotNull(findDescriptionFile(EduNames.TASK_MD))
  }

  fun `test wrong new task description file name`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    val newFileName = "incorrectFileName.txt"
    Messages.setTestInputDialog(testInputDialog(newFileName))
    val dataContext = dataContext(findDescriptionFile(EduNames.TASK_HTML) ?: error("Can't find `${EduNames.TASK_HTML}`"))
    val renameHandler = CCDescriptionFileRenameHandler()
    renameHandler.invoke(project, null, null, dataContext)

    assertEquals(DescriptionFormat.HTML, course.lessons[0].taskList[0].descriptionFormat)
    assertNull(findDescriptionFile(newFileName))
    assertNotNull(findDescriptionFile(EduNames.TASK_HTML))
  }

  private fun findDescriptionFile(name: String): VirtualFile? =
    LightPlatformTestCase.getSourceRoot().findFileByRelativePath("lesson1/task1/$name")

  private fun testInputDialog(message: String): TestInputDialog = object : TestInputDialog {

    override fun show(msg: String, validator: InputValidator?): String? =
      if (validator?.checkInput(message) == false) null else show(message)

    override fun show(msg: String?): String? = message
  }
}
