package com.jetbrains.edu.learning.stepik.course

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames

class StartStepikCourseAction : DumbAwareAction("Start Stepik Course") {

  override fun actionPerformed(e: AnActionEvent) {
    if (EduSettings.isLoggedIn()) {
      doImport()
    }
    else {
      val result = Messages.showOkCancelDialog("Stepik authorization is required to import courses", "Log in to Stepik",
                                               "Log in", "Cancel", null)
      if (result == Messages.OK) {
        StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
      }
    }
  }

  private fun doImport() {
    val course = importStepikCourse() ?: return
    JoinCourseDialog(course).show()
  }

  fun importStepikCourse(): Course? {
    val dialog = ImportStepikCourseDialog()
    if (!dialog.showAndGet()) {
      return null
    }
    val courseLink = dialog.courseLink()
    val course = StepikCourseConnector.getCourseInfoByLink(courseLink)
    if (course == null) {
      showFailedToAddCourseNotification()
      return null
    }

    // course language is already set if we opened idea compatible course by link
    if (course.isCompatible) {
      return course
    }

    val languages = getLanguagesUnderProgress(course)

    if (languages.isEmpty()) {
      Messages.showErrorDialog("No supported languages available for the course", "Failed to Import Course")
      return null
    }
    val language = chooseLanguageIfNeeded(languages, course) ?: return null
    course.type = String.format("%s%s %s", StepikNames.PYCHARM_PREFIX, JSON_FORMAT_VERSION, language.id)
    course.language = language.id
    return course
  }

  private fun chooseLanguageIfNeeded(languages: List<Language>, course: StepikCourse): Language? {
    return if (languages.size == 1) {
      languages[0]
    }
    else {
      val chooseLanguageDialog = ChooseStepikCourseLanguageDialog(languages, course.name)
      if (chooseLanguageDialog.showAndGet()) {
        chooseLanguageDialog.selectedLanguage()
      }
      else {
        null
      }
    }
  }

  private fun showFailedToAddCourseNotification() {
    Messages.showErrorDialog("Cannot find course on Stepik, please check if link is correct", "Failed to Load Stepik Course")
  }

  private fun getLanguagesUnderProgress(course: StepikCourse): List<Language> {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<List<Language>, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable {
          StepikCourseConnector.getSupportedLanguages(course)
        }
      }, "Getting Available Languages", true, null)
  }
}