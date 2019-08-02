package com.jetbrains.edu.learning.stepik.course

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility.*
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.stepik.StepikNames

class StartStepikCourseAction : DumbAwareAction("Start Stepik Course") {

  override fun actionPerformed(e: AnActionEvent) {
    doImport()
  }

  private fun doImport() {
    val course = importStepikCourse() ?: return
    JoinCourseDialog(course).show()
  }

  fun importStepikCourse(): Course? {
    val courseLink = showDialogAndGetCourseLink() ?: return null
    val course = StepikCourseConnector.getCourseInfoByLink(courseLink)
    if (course == null) {
      showFailedToAddCourseNotification(courseLink)
      return null
    }

    if (course is StepikCourse) {
      val language = getLanguageForStepikCourse(course)
      language?.let {
        course.type = "${StepikNames.PYCHARM_PREFIX}${JSON_FORMAT_VERSION} ${language.id}"
        course.language = language.id
        return course
      }
    }
    else if (isCompatibleEduCourse(course)) {
      return course
    }

    return null
  }

  private fun getLanguageForStepikCourse(course: StepikCourse): Language? {
    val languages = getLanguagesUnderProgress(course)

    if (languages.isEmpty()) {
      Messages.showErrorDialog("No supported languages available for '${course.name}'", "Failed to Import Course")
      return null
    }

    return chooseLanguageIfNeeded(languages, course)
  }

  private fun showDialogAndGetCourseLink(): String? {
    val dialog = ImportStepikCourseDialog()
    if (!dialog.showAndGet()) {
      return null
    }
    return dialog.courseLink()
  }

  private fun isCompatibleEduCourse(course: EduCourse): Boolean {
    return when (course.compatibility) {
      COMPATIBLE -> true

      UNSUPPORTED -> {
        showFailedImportCourseMessage("Looks like the programming language of '${course.name}' is not supported yet.")
        false
      }

      INCOMPATIBLE_VERSION -> {
        showFailedImportCourseMessage("'${course.name}' is supported in the latest plugin version only. Please, update the plugin.")
        false
      }
    }
  }

  private fun showFailedImportCourseMessage(message: String) = Messages.showErrorDialog(message, "Failed to Import Course")

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

  private fun showFailedToAddCourseNotification(courseLink: String) {
    Messages.showErrorDialog("Cannot find course on Stepik, please check if link is correct: $courseLink", "Failed to Load Stepik Course")
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