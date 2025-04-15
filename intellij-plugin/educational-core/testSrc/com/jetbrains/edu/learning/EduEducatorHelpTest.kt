package com.jetbrains.edu.learning

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ui.jcef.JBCefApp
import com.jetbrains.edu.coursecreator.ui.CCEducatorHelpProjectCloseListener.Companion.EDUCATOR_HELP_WAS_OPENED
import com.jetbrains.edu.coursecreator.ui.CCOpenEducatorHelp
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.courseFormat.CourseMode
import io.mockk.*
import org.junit.Test

class EduEducatorHelpTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    mockkStatic(JBCefApp::class)
    every { JBCefApp.isSupported() } returns true

    mockkObject(CCOpenEducatorHelp.Companion)
    every { CCOpenEducatorHelp.doOpen(any()) } returns Unit

    mockkObject(UserAgreementSettings)
    every { UserAgreementSettings.getInstance().isPluginAllowed } returns true

    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("file1.txt")
        }
      }
    }
    StudyTaskManager.getInstance(project).course = course
  }

  @Test
  fun `test educator help tab is reopened when flag is set to true`() {
    withFeature(EduExperimentalFeatures.EDUCATOR_HELP, true) {

      PropertiesComponent.getInstance(project).setValue(EDUCATOR_HELP_WAS_OPENED, true)

      val activity = spyk(EduStartupActivity())
      activity.runActivity(project)

      verify(exactly = 1) { CCOpenEducatorHelp.doOpen(project) }
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
    unmockkStatic(JBCefApp::class)
    unmockkObject(CCOpenEducatorHelp.Companion)
    unmockkObject(UserAgreementSettings)
  }
}
