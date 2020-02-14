package com.jetbrains.edu.learning.codeforces

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider.Companion.getLanguageIdAndVersion
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.newproject.ui.CoursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialogBase

class StartCodeforcesContestAction : DumbAwareAction("Start Codeforces Contest") {

  override fun actionPerformed(e: AnActionEvent) {
    val course = importCodeforcesContest() ?: return
    showCourseInfo(course)
  }

  private fun showCourseInfo(course: CodeforcesCourse) {
    // EDU-2664
    // We don't provide language settings for CPP due to mess with standards
    // Decided to do it well by adding toolchain select field
    val showLanguageSettings = course.languageID != EduNames.CPP

    object : JoinCourseDialogBase(course, CourseDisplaySettings(false, false, showLanguageSettings)) {
      override val allowViewAsEducatorAction: Boolean get() = false

      init {
        init()
      }
    }.show()
  }

  private fun importCodeforcesContest(): CodeforcesCourse? {
    val contestId = showDialogAndGetContestId() ?: return null
    val contestParameters = getContestParameters(contestId) ?: return null
    val codeforcesContest = getCodeforcesContestUnderProgress(contestParameters)
    if (codeforcesContest == null) showFailedToLoadContestInfoNotification(contestId)
    return codeforcesContest
  }

  private fun showDialogAndGetContestId(): Int? {
    val dialog = ImportCodeforcesContestDialog()
    if (!dialog.showAndGet()) {
      return null
    }
    return dialog.getContestId()
  }

  private fun getContestParameters(contestId: Int): ContestParameters? {
    val contestInfo = getContestInfoUnderProgress(contestId)
    if (contestInfo == null) {
      showFailedToFindContestNotification(contestId)
      return null
    }

    val codeforcesSettings = CodeforcesSettings.getInstance()
    var contestParameters: ContestParameters?
    if (codeforcesSettings.doNotShowLanguageDialog && codeforcesSettings.isSet()) {
      contestParameters = getContestParametersFromSettings(contestId)

      if (contestParameters != null && contestParameters.codeforcesLanguageRepresentation in contestInfo.availableLanguages) {
        return contestParameters
      }
    }

    contestParameters = showDialogAndGetContestParameters(contestInfo)
    return contestParameters
  }

  private fun getContestParametersFromSettings(contestId: Int): ContestParameters? {
    val codeforcesSettings = CodeforcesSettings.getInstance()

    val locale = (codeforcesSettings.preferableTaskTextLanguage ?: return null).locale
    val language = codeforcesSettings.preferableLanguage ?: return null
    val languageIdAndVersion = getLanguageIdAndVersion(language) ?: return null

    return ContestParameters(contestId, locale, language, languageIdAndVersion)
  }

  private fun getContestInfoUnderProgress(contestId: Int): ContestInformation? =
    ProgressManager.getInstance().runProcessWithProgressSynchronously<ContestInformation?, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable {
          CodeforcesConnector.getInstance().getContestInformation(contestId)
        }
      }, "Getting Available Languages", true, null)

  private fun showDialogAndGetContestParameters(contestInformation: ContestInformation): ContestParameters? {
    val contestName = contestInformation.name
    val contestLanguages = contestInformation.availableLanguages

    if (contestLanguages.isEmpty()) {
      showNoSupportedLanguagesForContestNotification(contestName)
      return null
    }

    val dialog = ChooseCodeforcesContestLanguagesDialog(contestInformation)
    if (!dialog.showAndGet()) {
      return null
    }

    val taskTextLanguage = dialog.selectedTaskTextLanguage()
    val language = dialog.selectedLanguage()
    val languageIdAndVersion = getLanguageIdAndVersion(language) ?: return null

    val codeforcesSettings = CodeforcesSettings.getInstance()
    if (dialog.isDoNotShowLanguageDialog()) {
      codeforcesSettings.preferableTaskTextLanguage = taskTextLanguage
      codeforcesSettings.preferableLanguage = language
      codeforcesSettings.doNotShowLanguageDialog = true
    }

    return ContestParameters(contestInformation.id, taskTextLanguage.locale, language, languageIdAndVersion)
  }

  private fun showFailedToLoadContestInfoNotification(contestId: Int) {
    Messages.showErrorDialog("Cannot get contest information on Codeforces, contest ID: $contestId",
                             "Failed to Load Available Contest Languages")
  }

  private fun showFailedToFindContestNotification(contestId: Int) {
    val contestUrl = CodeforcesContestConnector.getContestURLFromID(contestId)
    Messages.showErrorDialog("Cannot find contest on Codeforces, please check if the link is correct: $contestUrl",
                             "Failed to Load Codeforces Contest")
  }

  private fun showNoSupportedLanguagesForContestNotification(contestName: String) {
    Messages.showErrorDialog("No supported languages for `$contestName` contest, please choose another one",
                             "Failed to Load Codeforces Contest")
  }

  companion object {
    @VisibleForTesting
    fun getCodeforcesContestUnderProgress(contestParameters: ContestParameters): CodeforcesCourse? =
      ProgressManager.getInstance().runProcessWithProgressSynchronously<CodeforcesCourse?, RuntimeException>(
        {
          ProgressManager.getInstance().progressIndicator.isIndeterminate = true
          EduUtils.execCancelable {
            CodeforcesConnector.getInstance().getContest(contestParameters)
          }
        }, "Getting Contest Information", true, null)
  }
}