package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showTestUserDataNotification
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.update.MarketplaceUpdateChecker
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class MarketplaceStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (project.isDisposed || isUnitTestMode) return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return

    val res = JBAccountInfoService.getInstance()?.userData
    val text = if (res == null) {
      "UserData is null, service instance: ${JBAccountInfoService.getInstance()}"
    } else {
      "Current logged in user: ${res.loginName}, ${res.id}"
    }

    showTestUserDataNotification(project, text)

    if (!course.isMarketplaceRemote) return

    if (course.courseMode == CourseMode.EDUCATOR && course.generatedEduId == null) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val generatedId = MarketplaceConnector.getInstance().loadCourse(course.id).generatedEduId ?: course.generateEduId()
        course.generatedEduId = generatedId
        YamlFormatSynchronizer.saveRemoteInfo(course)
      }
      return
    }

    if (!project.isStudentProject()) return

    MarketplaceUpdateChecker.getInstance(project).check()

    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return

    submissionsManager.prepareSubmissionsContentWhenLoggedIn { MarketplaceSolutionLoader.getInstance(project).loadSolutionsInBackground() }
  }
}