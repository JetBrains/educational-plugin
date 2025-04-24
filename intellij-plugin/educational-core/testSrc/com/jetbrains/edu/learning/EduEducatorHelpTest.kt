package com.jetbrains.edu.learning

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.jcef.JBCefApp
import com.jetbrains.edu.coursecreator.ui.CCEducatorHelpProjectCloseListener
import com.jetbrains.edu.coursecreator.ui.CCEducatorHelpProjectCloseListener.Companion.EDUCATOR_HELP_WAS_OPENED
import com.jetbrains.edu.coursecreator.ui.CCOpenEducatorHelp
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import io.mockk.*
import org.junit.Test

class EduEducatorHelpTest : CourseReopeningTestBase<EmptyProjectSettings>() {

  override fun setUp() {
    super.setUp()

  }

  @Test
  fun `test educator help tab is reopened when flag is set to true`() {
    try {
      ApplicationManager.getApplication().invokeAndWait {
        mockkStatic(com.intellij.ui.jcef.JBCefApp::class)
        every { com.intellij.ui.jcef.JBCefApp.isSupported() } returns true
      }

      assert(JBCefApp.isSupported()) { "JBCefApp.isSupported() mock is not working" }


      mockkObject(CCOpenEducatorHelp.Companion)
      justRun { CCOpenEducatorHelp.doOpen(any()) }

      mockkConstructor(CCEducatorHelpProjectCloseListener::class)

      every { anyConstructed<CCEducatorHelpProjectCloseListener>().projectClosing(any()) } answers {
        val project = firstArg<Project>()
        PropertiesComponent.getInstance(project).setValue(EDUCATOR_HELP_WAS_OPENED, true)
      }
      val initialCourse = course(courseMode = CourseMode.EDUCATOR) {
        frameworkLesson("lesson") {
          eduTask("task") {
            taskFile("Task.kt", "fun foo() = 12")
          }
        }
      }

      val testOnFirstOpen: (Project) -> Unit = {
      }

      val secondOpen: (Project) -> Unit = { project ->
        PlatformTestUtil.waitWhileBusy { EduProjectServiceForTests.isReopenEditorHelpExecuted() }
        verify(exactly = 1) { CCOpenEducatorHelp.doOpen(project) }
      }

      openStudentProjectThenReopenStudentProject(initialCourse, testOnFirstOpen, secondOpen)
    }
    finally {
      tearDown()
    }
  }

  @Test
  fun `test educator help tab is not reopened when flag is set to false`() {
    withFeature(EduExperimentalFeatures.EDUCATOR_HELP, true) {
      PropertiesComponent.getInstance(project).setValue(EDUCATOR_HELP_WAS_OPENED, false)

      val activity = spyk(EduStartupActivity())
      activity.runActivity(project)

      verify(exactly = 0) { CCOpenEducatorHelp.doOpen(project) }
    }

  }

  override fun tearDown() {
    super.tearDown()
    unmockkAll()
    EduProjectServiceForTests.reset()
  }

  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

}
