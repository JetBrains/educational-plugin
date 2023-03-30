package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.StepikUpdateChecker
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.SUBMISSIONS_TAB

class StepikStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (!EduUtils.isEduProject(project)) return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return

    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return

    val updateChecker = StepikUpdateChecker.getInstance(project)
    StepikConnector.getInstance().setSubmissionTabListener(object : EduLogInListener {
      override fun userLoggedIn() {
        if (project.isDisposed) return
        if (!course.isStepikPublic) {
          updateChecker.check()
        }
      }

      override fun userLoggedOut() {
        if (project.isDisposed) return
        TaskDescriptionView.getInstance(project).updateTab(SUBMISSIONS_TAB)
      }
    })

    updateChecker.check()
    val currentUser = EduSettings.getInstance().user
    if (currentUser == null) {
      showBalloon(project)
    }
  }

  private fun showBalloon(project: Project) {
    val frame = WindowManager.getInstance().getIdeFrame(project) ?: return
    val statusBar = frame.statusBar ?: return
    val widget = statusBar.getWidget(StepikWidget.ID) as? CustomStatusBarWidget ?: return
    val widgetComponent = widget.component ?: return
    val builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
      EduCoreBundle.message("stepik.log.in.message"),
      MessageType.INFO,
      null
    )
    builder.setClickHandler({ StepikConnector.getInstance().doAuthorize() }, true)
    builder.setHideOnClickOutside(true)
    builder.setCloseButtonEnabled(true)
    builder.setHideOnCloseClick(true)
    val balloon = builder.createBalloon()
    balloon.showInCenterOf(widgetComponent)
  }

  companion object {
    private val LOG: Logger = logger<StepikStartupActivity>()
  }
}
