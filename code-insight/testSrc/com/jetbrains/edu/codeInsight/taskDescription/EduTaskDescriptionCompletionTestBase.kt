package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl
import com.jetbrains.edu.codeInsight.EduCompletionTextFixture
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory

abstract class EduTaskDescriptionCompletionTestBase(format: DescriptionFormat) : EduTaskDescriptionTestBase(format) {

  private lateinit var completionFixture: EduCompletionTextFixture

  override fun setUp() {
    super.setUp()
    completionFixture = EduCompletionTextFixture(myFixture)
    completionFixture.setUp()
    registerTaskDescriptionToolWindow()
  }

  override fun tearDown() {
    completionFixture.tearDown()
    super.tearDown()
  }

  override fun createCourse() {
    courseWithFiles {
      lesson("lesson1") {
        eduTask("task1", taskDescriptionFormat = taskDescriptionFormat) {
          taskFile("Task.txt")
          taskFile("foo/Task2.txt")
          dir("tests") {
            taskFile("test.txt")
          }
        }
      }
      section("section1") {
        lesson("lesson2") {
          eduTask("tas2", taskDescriptionFormat = taskDescriptionFormat) {
            taskFile("Task.txt")
          }
        }
      }
    }
  }

  protected open fun doTest(linkBefore: String, linkAfter: String) {
    val taskDescriptionFile = findFile("lesson1/task1/${taskDescriptionFormat.descriptionFileName}")
    completionFixture.doSingleCompletion(taskDescriptionFile, linkBefore.withDescriptionFormat(), linkAfter.withDescriptionFormat())
  }

  protected open fun checkNoCompletion(link: String) {
    val taskDescriptionFile = findFile("lesson1/task1/${taskDescriptionFormat.descriptionFileName}")
    completionFixture.checkNoCompletion(taskDescriptionFile, link.withDescriptionFormat())
  }

  // In tests, tool windows are not registered by default
  // Let's register at least Task Description tool window
  private fun registerTaskDescriptionToolWindow() {
    val toolWindowManager = ToolWindowManager.getInstance(project) as ToolWindowHeadlessManagerImpl
    if (toolWindowManager.getToolWindow("Task") == null) {
      for (bean in ToolWindowEP.EP_NAME.extensionList) {
        if (bean.id == TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW) {
          toolWindowManager.doRegisterToolWindow(bean.id)
          Disposer.register(testRootDisposable) {
            @Suppress("DEPRECATION")
            toolWindowManager.unregisterToolWindow(bean.id)
          }
        }
      }
    }
  }
}
