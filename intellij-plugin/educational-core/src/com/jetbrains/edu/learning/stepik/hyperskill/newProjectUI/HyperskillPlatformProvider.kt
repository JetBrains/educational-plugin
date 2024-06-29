package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.HyperskillCourseAdvertiser
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenProjectStageRequest
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectAction
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class HyperskillPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(HyperskillPlatformProvider())
}

class HyperskillPlatformProvider : CoursesPlatformProvider() {
  override val name: String = EduNames.JBA

  override val icon: Icon get() = EducationalCoreIcons.Platform.Tab.JetBrainsAcademy

  override fun createPanel(scope: CoroutineScope, disposable: Disposable): CoursesPanel = HyperskillCoursesPanel(
    this, scope,
    disposable
  )

  override fun joinAction(
    courseInfo: CourseCreationInfo,
    courseMode: CourseMode,
    coursePanel: CoursePanel
  ) {

    val course = courseInfo.course
    if (course is HyperskillCourse) {
      computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.stages")) {
        HyperskillConnector.getInstance().loadStages(course)
      }
      super.joinAction(courseInfo, courseMode, coursePanel)
      return
    }

    val isOpened = HyperskillProjectAction.openHyperskillProject { errorMessage ->
      Messages.showErrorDialog(errorMessage.message, EduCoreBundle.message("hyperskill.failed.to.open.project"))
      logger<HyperskillPlatformProvider>().warn("Joining a course resulted in an error: ${errorMessage.message}. The error was shown inside an error dialog.")
    }

    if (isOpened) {
      val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, coursePanel)
      dialog?.dialogWrapper?.close(DialogWrapper.OK_EXIT_CODE)
    }
  }

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    val selectedProject = getSelectedProject()
    val storedCourses = CoursesStorage.getInstance().state.courses
      .filter { it.type == HYPERSKILL && it.id != selectedProject?.id }
      .map { it.toCourse() }

    val courses = run { listOfNotNull(selectedProject?.course) + storedCourses }
      .ifEmpty {
        // if no JB Academy content to offer, advertise it
        listOf(HyperskillCourseAdvertiser())
      }

    return CoursesGroup.fromCourses(courses)
  }

  private val HyperskillProject.course: HyperskillCourse?
    get() = HyperskillOpenInIdeRequestHandler.createHyperskillCourse(
      HyperskillOpenProjectStageRequest(id, null),
      language,
      this
    ).onError { null }

  private fun getSelectedProject(): HyperskillProject? {
    val currentUserInfo = HyperskillConnector.getInstance().getCurrentUserInfo() ?: return null
    val projectId = currentUserInfo.hyperskillProjectId ?: return null

    return HyperskillConnector.getInstance().getProject(projectId).onError {
      return null
    }
  }
}