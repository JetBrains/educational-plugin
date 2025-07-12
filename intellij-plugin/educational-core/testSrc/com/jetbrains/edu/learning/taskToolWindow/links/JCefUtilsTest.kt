package com.jetbrains.edu.learning.taskToolWindow.links

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.junit.Test

class JCefUtilsTest : EduTestCase() {

  @Test
  fun `test about_blank`() {
    val jCefToolWindowLinkHandler = JCefToolWindowLinkHandler(project)
    assertFalse(jCefToolWindowLinkHandler.process("about:blank"))
  }

  @Test
  fun `test external links`() {
    val jCefToolWindowLinkHandler = JCefToolWindowLinkHandler(project)
    assertTrue(jCefToolWindowLinkHandler.process("https://www.youtube.com/watch?v=FWukd9fsRro"))
    assertTrue(jCefToolWindowLinkHandler.process("https://google.com"))
  }

  @Test
  fun `test external link with ref`() {
    val jCefToolWindowLinkHandler = JCefToolWindowLinkHandler(project)
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

    ToolWindowLinkHandler(project).process("file://$pathToFile")
    val currentTask = project.getCurrentTask()
    assertEquals("task_name2", currentTask?.name)
  }

  @Test
  fun `test psi element`() {
    val jCefToolWindowLinkHandler = JCefToolWindowLinkHandler(project)
    assertTrue(jCefToolWindowLinkHandler.process("file:///jbcefbrowser/psi_element://java.lang.String#contains"))
    assertNull(project.getCurrentTask())
  }
}