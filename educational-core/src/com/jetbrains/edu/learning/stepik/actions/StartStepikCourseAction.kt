package com.jetbrains.edu.learning.stepik.actions

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.stepik.StepikConnector
import com.jetbrains.edu.learning.stepik.StepikUtils
import com.jetbrains.edu.learning.stepik.StepikUtils.getCourseFormat
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import com.jetbrains.edu.learning.stepik.newProject.ChooseStepikCourseLanguageDialog
import com.jetbrains.edu.learning.stepik.newProject.ImportStepikCourseDialog

class StartStepikCourseAction : DumbAwareAction("Start Stepik Course") {
  override fun actionPerformed(e: AnActionEvent) {
    if (!EduSettings.isLoggedIn()) {
      val result = Messages.showOkCancelDialog("Stepik authorization is required to import courses", "Log in to Stepik", "Log in", "Cancel",
                                               null)
      if (result == Messages.OK) {
        val myBusConnection = ApplicationManager.getApplication().messageBus.connect()
        myBusConnection.subscribe(EduSettings.SETTINGS_CHANGED, EduSettings.StudySettingsListener {
          if (EduSettings.isLoggedIn()) {
            runInEdt {
              doImport()
            }
          }
        })
        StepikConnector.doAuthorize { StepikUtils.showOAuthDialog() }
      }
      return
    }
    doImport()
  }

  private fun doImport() {
    val course = importStepikCourse() ?: return
    JoinCourseDialog(course).show()
  }

  fun importStepikCourse(): StepikCourse? {
    val dialog = ImportStepikCourseDialog()
    if (!dialog.showAndGet()) {
      return null
    }
    val courseLink = dialog.courseLink()
    val user = EduSettings.getInstance().user!!
    val course = StepikConnector.getCourseInfoByLink(user, courseLink)
    val languages = getLanguagesUnderProgress(course)

    if (languages.isEmpty()) {
      Messages.showErrorDialog("No supported languages available for the course", "Failed to Import Course")
      return null
    }
    if (course == null) {
      showFailedToAddCourseNotification()
      return null
    }
    val language = chooseLanguageIfNeeded(languages, course) ?: return null
    course.courseFormat = getCourseFormat(language.id)
    course.language = language.id
    return course
  }

  private fun chooseLanguageIfNeeded(languages: List<Language>,
                                     course: StepikCourse): Language? {
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
    Messages.showErrorDialog("Cannot add course from Stepik, please check if link is correct", "Failed to Add Stepik Course")
  }

  private fun getLanguagesUnderProgress(course: StepikCourse): List<Language> {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<List<Language>, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable {
          StepikConnector.getSupportedLanguages(
            course)
        }
      }, "Getting Available Languages", true, null)
  }
}