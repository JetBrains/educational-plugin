package com.jetbrains.edu.coursecreator.actions.marketplace.courseStorage

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.coursecreator.CCUtils.addGluingSlash
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.CCUtils.prepareForUpload
import com.jetbrains.edu.coursecreator.archive.CourseArchiveCreator
import com.jetbrains.edu.coursecreator.archive.showNotification
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.courseStorage.COURSE_STORAGE
import com.jetbrains.edu.learning.marketplace.courseStorage.api.CourseStorageConnector
import com.jetbrains.edu.learning.marketplace.isFromCourseStorage
import com.jetbrains.edu.learning.messages.EduCoreBundle.message

private val uploadCourseActionText: @NlsActions.ActionText String
  get() = message("action.push.course.storage.upload.text")
private val updateCourseActionText: @NlsActions.ActionText String
  get() = message("action.push.course.storage.update.text")

class CourseStoragePushCourse : DumbAwareAction(addGluingSlash(uploadCourseActionText, updateCourseActionText)) {
  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return

    if (!isActionVisible(project, course)) return

    presentation.setText {
      if (course.isMarketplaceRemote) {
        updateCourseActionText
      }
      else {
        uploadCourseActionText
      }
    }

    presentation.isEnabledAndVisible = true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (!isActionVisible(project, course)) return

    course.prepareForUpload(project)
    uploadCourse(project, course)
  }

  private fun uploadCourse(project: Project, course: EduCourse) {
    val tempFile = FileUtil.createTempFile("$COURSE_STORAGE-${course.name}-${course.marketplaceCourseVersion}", ".zip", true)
    val error = CourseArchiveCreator(project, tempFile.toPath()).createArchive(course)
    if (error != null) {
      error.showNotification(project, message("error.failed.to.create.course.archive.notification.title"))
      return
    }

    if (course.isMarketplaceRemote) {
      CourseStorageConnector.getInstance().uploadCourseUpdate(project, course, tempFile)
    }
    else {
      CourseStorageConnector.getInstance().uploadNewCourse(project, course, tempFile)
    }
  }

  private fun isActionVisible(project: Project, course: EduCourse): Boolean {
    return isCourseCreator(project) &&
           isFeatureEnabled(EduExperimentalFeatures.COURSE_STORAGE) &&
           isUnderJBEmail() &&
           course.isMarketplace &&
           (course.id == 0 || course.isFromCourseStorage())
  }

  private fun isUnderJBEmail(): Boolean {
    val authConnector = MarketplaceConnector.getInstance()
    val account = authConnector.account ?: return false
    return account.userInfo.email.endsWith(JB_EMAIL_SUFFIX)
  }

  companion object {
    private const val JB_EMAIL_SUFFIX = "@jetbrains.com"

    const val ACTION_ID: String = "Educational.Educator.CourseStoragePushCourse"
  }
}