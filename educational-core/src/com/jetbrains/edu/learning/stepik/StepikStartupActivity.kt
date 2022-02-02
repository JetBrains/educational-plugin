package com.jetbrains.edu.learning.stepik

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
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

    if (EduSettings.getInstance().user != null) {
      submissionsManager.prepareSubmissionsContent {
        loadSolutionsFromStepik(project, course)
      }
    }

    val updateChecker = StepikUpdateChecker.getInstance(project)
    StepikConnector.getInstance().setSubmissionTabListener(object : EduLogInListener {
      override fun userLoggedIn() {
        if (project.isDisposed) return
        submissionsManager.prepareSubmissionsContent()
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
    selectStep(project, course)
  }

  private fun showBalloon(project: Project) {
    val frame = WindowManager.getInstance().getIdeFrame(project) ?: return
    val statusBar = frame.statusBar ?: return
    val widget = statusBar.getWidget(StepikWidget.ID) as? CustomStatusBarWidget ?: return
    val widgetComponent = widget.component ?: return
    val builder = JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder("<a href=\"\">Log in</a> to synchronize your study progress", MessageType.INFO, null)
    builder.setClickHandler({ StepikConnector.getInstance().doAuthorize() }, true)
    builder.setHideOnClickOutside(true)
    builder.setCloseButtonEnabled(true)
    builder.setHideOnCloseClick(true)
    val balloon = builder.createBalloon()
    balloon.showInCenterOf(widgetComponent)
  }

  private fun loadSolutionsFromStepik(project: Project, course: EduCourse) {
    if (project.isDisposed || !course.isStepikRemote) return

    val component = PropertiesComponent.getInstance(project)
    if (component.getBoolean(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY)) {
      component.setValue(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY, false)
      return
    }
    try {
      StepikSolutionsLoader.getInstance(project).loadSolutionsInBackground()
      EduCounterUsageCollector.synchronizeCourse(course, EduCounterUsageCollector.SynchronizeCoursePlace.PROJECT_REOPEN)
    }
    catch (e: Exception) {
      LOG.warn(e)
    }
  }

  private fun selectStep(project: Project, course: Course) {
    val stepId = course.dataHolder.getUserData(STEP_ID)
    if (stepId != null) {
      EduUtils.navigateToStep(project, course, stepId)
    }
  }

  companion object {
    private val LOG: Logger = logger<StepikStartupActivity>()

    @JvmStatic
    val STEP_ID = Key.create<Int>("STEP_ID")
  }
}
