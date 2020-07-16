package com.jetbrains.edu.learning.stepik

import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
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
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.synchronizeCourse
import com.jetbrains.edu.learning.stepik.hyperskill.EduCourseUpdateChecker
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

// educational-core.xml
class StepikProjectComponent(private val myProject: Project) : ProjectComponent {
  override fun projectOpened() {
    if (myProject.isDisposed || !EduUtils.isEduProject(myProject)) {
      return
    }
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized {
      val course = StudyTaskManager.getInstance(myProject).course
      val submissionsManager = SubmissionsManager.getInstance(myProject)
      if (course is EduCourse && submissionsManager.submissionsSupported()) {
        val updateChecker = EduCourseUpdateChecker.getInstance(myProject)
        if (EduSettings.getInstance().user != null) {
          submissionsManager.prepareSubmissionsContent {
            loadSolutionsFromStepik(course)
          }
        }
        else {
          val busConnection = myProject.messageBus.connect(myProject)
          busConnection.subscribe(EduSettings.SETTINGS_CHANGED, object : EduLogInListener {
            override fun userLoggedIn() {
              if (EduSettings.getInstance().user == null) {
                return
              }
              submissionsManager.prepareSubmissionsContent {}
              if (!course.isPublic) {
                updateChecker.check()
              }
            }

            override fun userLoggedOut() {
              TaskDescriptionView.getInstance(myProject).updateSubmissionsTab()
            }
          })
        }
        updateChecker.check()
        val currentUser = EduSettings.getInstance().user
        if (currentUser == null) {
          showBalloon()
        }
        selectStep(course)
      }
    }
  }

  private fun showBalloon() {
    val frame = WindowManager.getInstance().getIdeFrame(myProject) ?: return
    val statusBar = frame.statusBar ?: return
    val widget = statusBar.getWidget(StepikWidget.ID) as? CustomStatusBarWidget ?: return
    val widgetComponent = widget.component ?: return
    val redirectUrl = StepikAuthorizer.getOAuthRedirectUrl()
    val authLnk = StepikAuthorizer.createOAuthLink(redirectUrl)
    val builder = JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder("<a href=\"\">Log in</a> to synchronize your study progress", MessageType.INFO,
                                    null)
    builder.setClickHandler({ BrowserUtil.browse(authLnk) }, true)
    builder.setHideOnClickOutside(true)
    builder.setCloseButtonEnabled(true)
    builder.setHideOnCloseClick(true)
    val balloon = builder.createBalloon()
    balloon.showInCenterOf(widgetComponent)
  }

  private fun loadSolutionsFromStepik(course: Course) {
    if (!myProject.isDisposed && course is EduCourse && course.isRemote) {
      if (PropertiesComponent.getInstance(myProject).getBoolean(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY)) {
        PropertiesComponent.getInstance(myProject).setValue(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY, false)
        return
      }
      try {
        StepikSolutionsLoader.getInstance(myProject).loadSolutionsInBackground()
        synchronizeCourse(EduCounterUsageCollector.SynchronizeCoursePlace.PROJECT_REOPEN)
      }
      catch (e: Exception) {
        LOG.warn(e.message)
      }
    }
  }

  private fun selectStep(course: Course) {
    val stepId = course.getUserData(STEP_ID)
    if (stepId != null) {
      EduUtils.navigateToStep(myProject, course, stepId)
    }
  }

  override fun getComponentName(): String {
    return "StepikProjectComponent"
  }

  companion object {
    private val LOG = Logger.getInstance(StepikProjectComponent::class.java)
    @JvmStatic
    val STEP_ID = Key.create<Int>("STEP_ID")
  }
}
