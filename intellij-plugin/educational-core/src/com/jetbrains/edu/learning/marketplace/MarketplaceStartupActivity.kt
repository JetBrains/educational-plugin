package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseListener
import com.intellij.openapi.startup.StartupActivity
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.isHeadlessEnvironment
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.update.MarketplaceUpdateChecker
import com.jetbrains.edu.learning.marketplace.userAgreement.UserAgreementDialog
import com.jetbrains.edu.learning.marketplace.userAgreement.UserAgreementSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.DownloadCourseContext.OTHER
import com.jetbrains.edu.learning.submissions.SubmissionSettings
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.UserAgreementState
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class MarketplaceStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (project.isDisposed || isUnitTestMode) return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return

    if (course.courseMode == CourseMode.EDUCATOR && course.generatedEduId == null) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val generatedId = MarketplaceConnector.getInstance().loadCourse(course.id, OTHER).generatedEduId ?: course.generateEduId()
        course.generatedEduId = generatedId
        YamlFormatSynchronizer.saveRemoteInfo(course)
      }
      return
    }

    if (!project.isStudentProject()) return

    val marketplaceConnector = MarketplaceConnector.getInstance()

    if (!isHeadlessEnvironment) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val isToShowUserAgreementDialog = marketplaceConnector.isLoggedIn() &&
                                          !UserAgreementSettings.getInstance().isDialogShown &&
                                          MarketplaceSubmissionsConnector.getInstance().getUserAgreementState() == UserAgreementState.NOT_SHOWN

        if (isToShowUserAgreementDialog) {
          runInEdt { UserAgreementDialog.showUserAgreementDialog(project) }
        }
      }
    }

    MarketplaceUpdateChecker.getInstance(project).check()

    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return

    val solutionsLoader = MarketplaceSolutionLoader.getInstance(project)
    submissionsManager.prepareSubmissionsContentWhenLoggedIn { solutionsLoader.loadSolutionsInBackground() }

    project.messageBus.connect().subscribe(ProjectCloseListener.TOPIC, object : ProjectCloseListener {
      override fun projectClosing(project: Project) {
        if (!SubmissionSettings.getInstance(project).stateOnClose) return

        // We can't make it asynchronous since we need project to get course data
        runWithModalProgressBlocking(project, EduCoreBundle.message("save.course.state.progress.title")) {
          val loggedIn = marketplaceConnector.isLoggedIn()
          if (loggedIn) {
            blockingContext {
              MarketplaceSubmissionsConnector.getInstance().saveCurrentState(project, course)
            }
          }
        }
      }
    })
  }
}