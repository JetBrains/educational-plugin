package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.Compatible
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.IncompatibleVersion
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.stepik.StepikLanguage
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.JBU_DEFAULT_URL

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
    if (course.isAdaptive) {
      showAdaptiveCoursesAreNotSupportedNotification(course.name)
      return null
    }

    if (course is StepikCourse) {
      val language = getLanguageForStepikCourse(course)
      language?.let {
        course.type = "${StepikNames.PYCHARM_PREFIX}${JSON_FORMAT_VERSION} ${language.id}"
        course.language = "${language.id} ${language.version}".trim()
        return course
      }
    }
    else if (isCompatibleEduCourse(course)) {
      return course
    }

    return null
  }

  private fun getLanguageForStepikCourse(course: StepikCourse): StepikLanguage? {
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
      Compatible -> true
      IncompatibleVersion -> {
        showFailedImportCourseMessage("'${course.name}' is supported in the latest plugin version only. Please, update the plugin.")
        false
      }
      // TODO: allow to install/enable plugins here
      else -> {
        showFailedImportCourseMessage("Looks like the programming language of '${course.name}' is not supported yet.")
        false
      }
    }
  }

  private fun showFailedImportCourseMessage(message: String) = Messages.showErrorDialog(message, "Failed to Import Course")

  private fun chooseLanguageIfNeeded(languages: List<StepikLanguage>, course: StepikCourse): StepikLanguage? {
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

  private fun showAdaptiveCoursesAreNotSupportedNotification(courseName: String) {
    Messages.showErrorDialog(EduCoreBundle.message("error.adaptive.courses.not.supported.message", courseName, JBU_DEFAULT_URL, EduNames.JBA),
                             EduCoreBundle.message("error.adaptive.courses.not.supported.title"))
  }

  private fun showFailedToAddCourseNotification(courseLink: String) {
    Messages.showErrorDialog("Cannot find course on Stepik, please check if link is correct: $courseLink", "Failed to Load Stepik Course")
  }

  private fun getLanguagesUnderProgress(course: StepikCourse): List<StepikLanguage> {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<List<StepikLanguage>, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable {
          StepikCourseConnector.getSupportedLanguages(course)
        }
      }, "Getting Available Languages", true, null)
  }
}