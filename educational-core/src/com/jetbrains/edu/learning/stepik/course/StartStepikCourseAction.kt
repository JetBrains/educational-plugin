package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.Companion.validateLanguage
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.Compatible
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.IncompatibleVersion
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.JBA_DEFAULT_URL

class StartStepikCourseAction : StartCourseAction(StepikNames.STEPIK) {
  override val dialog: ImportCourseDialog
    get() = ImportStepikCourseDialog(courseConnector)
  override val courseConnector: CourseConnector = StepikCourseConnector

  override fun importCourse(): EduCourse? {
    val course = super.importCourse() ?: return null
    if (course is StepikCourse && course.isAdaptive) {
      showAdaptiveCoursesAreNotSupportedNotification(course.name)
      return null
    }

    if (course is StepikCourse) {
      val language = getLanguageForStepikCourse(course)
      language?.let {
        course.type = "${StepikNames.PYCHARM_PREFIX}${JSON_FORMAT_VERSION} ${language.id}"
        course.programmingLanguage = "${language.id} ${language.version}".trim()
      }
      course.validateLanguage().onError {
        Messages.showErrorDialog(it, message("error.failed.to.import.course"))
        return null
      }
      if (language?.language == null || language.language?.id !in EduConfiguratorManager.supportedEduLanguages) return null
      return course
    }
    else if (isCompatibleEduCourse(course)) {
      return course
    }

    return null
  }

  private fun getLanguageForStepikCourse(course: StepikCourse): EduLanguage? {
    val languages = getLanguagesUnderProgress(course).onError {
      Messages.showErrorDialog(it, message("error.failed.to.import.course"))
      return null
    }

    if (languages.isEmpty()) {
      Messages.showErrorDialog(message("error.no.supported.languages", course.name), message("error.failed.to.import.course"))
      return null
    }

    return chooseLanguageIfNeeded(languages, course)
  }

  private fun isCompatibleEduCourse(course: EduCourse): Boolean {
    return when (course.compatibility) {
      Compatible -> true
      IncompatibleVersion -> {
        showFailedImportCourseMessage(message("error.update.plugin", course.name))
        false
      }
      // TODO: allow to install/enable plugins here
      else -> {
        showFailedImportCourseMessage(message("error.programming.language.not.supported", course.name))
        false
      }
    }
  }

  private fun chooseLanguageIfNeeded(languages: List<EduLanguage>, course: StepikCourse): EduLanguage? {
    return if (languages.size == 1) {
      languages[0]
    }
    else {
      val supportedLanguages = languages.filter { it.language?.id in EduConfiguratorManager.supportedEduLanguages }
      val chooseLanguageDialog = ChooseStepikCourseLanguageDialog(supportedLanguages, course.name)
      if (chooseLanguageDialog.showAndGet()) {
        chooseLanguageDialog.selectedLanguage()
      }
      else {
        null
      }
    }
  }

  private fun showAdaptiveCoursesAreNotSupportedNotification(courseName: String) {
    Messages.showErrorDialog(message("error.adaptive.courses.not.supported.message", courseName, JBA_DEFAULT_URL, EduNames.JBA),
                             message("error.adaptive.courses.not.supported.title"))
  }

  private fun getLanguagesUnderProgress(course: StepikCourse): Result<List<EduLanguage>, String> {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<Result<List<EduLanguage>, String>, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable {
          StepikCourseConnector.getSupportedLanguages(course)
        }
      }, message("stepik.getting.languages"), true, null)
  }
}