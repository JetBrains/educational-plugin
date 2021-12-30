package com.jetbrains.edu.learning.taskDescription.ui

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.navigation.NavigationUtils
import junit.framework.TestCase
import org.junit.Test

class jcefUtilsTest : EduTestCase() {

  @Test
  fun `test about_blank`() {
    val jCefToolWindowLinkHandler = JCefToolWindowLinkHandler(project)
    TestCase.assertFalse(jCefToolWindowLinkHandler.process("about:blank"))
  }

  @Test
  fun `test youtube link`() {
    val jCefToolWindowLinkHandler = JCefToolWindowLinkHandler(project)
    TestCase.assertFalse(jCefToolWindowLinkHandler.process("https://www.youtube.com/watch?v=FWukd9fsRro"))
  }

  @Test
  fun `test processInFileLink`() {
    val pathToFile = "lesson2/task_name2/inside.txt"
    courseWithFiles {
      lesson("lesson1") {
        eduTask("task_name1", taskDescription = "[myLinkToLesson2](file://$pathToFile)") {
          taskFile("inside.txt")
        }
      }

      lesson("lesson2") {
        eduTask("task_name2") {
          taskFile("inside.txt")
        }
      }
    }

    val task = findTask(0, 0)
    NavigationUtils.navigateToTask(project, task)

    ToolWindowLinkHandler.processFileLink(project, "file://$pathToFile")
    val currentTask = EduUtils.getCurrentTask(project)
    assertEquals("task_name2", currentTask?.name)
  }
}