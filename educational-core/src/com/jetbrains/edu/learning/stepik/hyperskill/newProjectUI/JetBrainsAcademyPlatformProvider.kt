package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStageRequest
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectAction
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectOpener
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import icons.EducationalCoreIcons
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class JetBrainsAcademyPlatformProvider : CoursesPlatformProvider() {
  override val name: String = EduNames.JBA

  override val icon: Icon get() = EducationalCoreIcons.JB_ACADEMY_TAB

  override fun createPanel(scope: CoroutineScope): CoursesPanel = JetBrainsAcademyCoursesPanel(this, scope)

  override fun joinAction(courseInfo: CourseInfo,
                          courseMode: CourseMode,
                          coursePanel: CoursePanel) {

    if (courseInfo.course is HyperskillCourse) {
      super.joinAction(courseInfo, courseMode, coursePanel)
      return
    }
    val account = HyperskillSettings.INSTANCE.account ?: return

    val isOpened = HyperskillProjectAction.openHyperskillProject(account) { errorMessage ->
      Messages.showErrorDialog(errorMessage, EduCoreBundle.message("hyperskill.failed.to.open.project"))
    }

    if (isOpened) {
      val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, coursePanel)
      dialog?.dialogWrapper?.close(DialogWrapper.OK_EXIT_CODE)
    }
  }

  override suspend fun loadCourses(): List<Course> {
    val hyperskillProject = getSelectedProject() ?: return listOf(JetBrainsAcademyCourse())
    val hyperskillCourse = HyperskillProjectOpener.createHyperskillCourse(HyperskillOpenStageRequest(hyperskillProject.id, null),
                                                                          hyperskillProject.language,
                                                                          hyperskillProject).onError { return emptyList() }
    return listOf(hyperskillCourse)
  }

  private fun getSelectedProject(): HyperskillProject? {
    val account = HyperskillSettings.INSTANCE.account ?: return null
    val currentUser = HyperskillConnector.getInstance().getCurrentUser(account) ?: return null
    account.userInfo = currentUser
    val projectId = account.userInfo.hyperskillProjectId ?: return null

    return HyperskillConnector.getInstance().getProject(projectId).onError {
      return null
    }
  }
}