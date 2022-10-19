package com.jetbrains.edu.learning.taskDescription.ui

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.handlers.JcefToolWindowLinkHandler
import junit.framework.TestCase
import org.junit.Test

class jcefUtilsTest : EduTestCase() {

  @Test
  fun `test about_blank`() {
    val jCefToolWindowLinkHandler = JcefToolWindowLinkHandler(project)
    assertFalse(jCefToolWindowLinkHandler.process("about:blank"))
  }

  @Test
  fun `test youtube link`() {
    val jCefToolWindowLinkHandler = JcefToolWindowLinkHandler(project)
    assertFalse(jCefToolWindowLinkHandler.process("https://www.youtube.com/watch?v=FWukd9fsRro"))
  }


  @Test
  fun `test youtube link with ref`() {
    val jCefToolWindowLinkHandler = JcefToolWindowLinkHandler(project)
    assertFalse(jCefToolWindowLinkHandler.process("https://www.youtube.com/watch?v=FWukd9fsRro", "https://google.account"))
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

  @Test
  fun `test psi Element`() {
    val jCefToolWindowLinkHandler = JcefToolWindowLinkHandler(project)
    assertTrue(jCefToolWindowLinkHandler.process("file:///jbcefbrowser/psi_element://java.lang.String#contains"))
    TestCase.assertNull(EduUtils.getCurrentTask(project))
  }
}