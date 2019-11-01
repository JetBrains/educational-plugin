package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider.Companion.getLanguageIdAndVersion
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.newproject.ui.CoursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialogBase

class StartCodeforcesContestAction : DumbAwareAction("Start Codeforces Contest") {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = isFeatureEnabled(EduExperimentalFeatures.CODEFORCES)
  }

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
    val (contestId, contestLanguage) = showDialogAndGetContestIdAndLanguages() ?: return null

    val contestNameAndLanguages = getContestShortInfoUnderProgress(contestId)
    if (contestNameAndLanguages == null) {
      showFailedToLoadContestProgrammingLanguagesNotification(contestId)
      return null
    }

    val languageId = showDialogAndGetLanguageIdAndVersion(contestNameAndLanguages.name, contestNameAndLanguages.languages) ?: return null
    val contestURLInfo = ContestURLInfo(contestId, contestLanguage, languageId)

    val contestInfo = getContestInfoUnderProgress(contestURLInfo)
    if (contestInfo == null) showFailedToAddCourseNotification(contestURLInfo.url)
    return contestInfo
  }

  private fun showDialogAndGetContestIdAndLanguages(): Pair<Int, String>? {
    val dialog = ImportCodeforcesContestDialog()
    if (!dialog.showAndGet()) {
      return null
    }
    return dialog.getContestIdAndLanguage()
  }

  private fun showDialogAndGetLanguageIdAndVersion(contestName: String, contestLanguages: List<String>): String? {
    val dialog = ChooseCodeforcesContestLanguageDialog(contestName, contestLanguages)
    when (contestLanguages.size) {
      0 -> {
        showNoSupportedLanguagesForContestNotification(contestName)
        return null
      }
      1 -> return getLanguageIdAndVersion(contestLanguages[0])
      else -> {
        if (!dialog.showAndGet()) {
          return null
        }
        return getLanguageIdAndVersion(dialog.selectedProgrammingLanguage())
      }
    }
  }

  private fun getContestShortInfoUnderProgress(contestId: Int): ContestShortInfo? =
    ProgressManager.getInstance().runProcessWithProgressSynchronously<ContestShortInfo?, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable {
          CodeforcesConnector.getInstance().getContestShortInfo(contestId)
        }
      }, "Getting Available Languages", true, null)

  private fun getContestInfoUnderProgress(contestURLInfo: ContestURLInfo): CodeforcesCourse? =
    ProgressManager.getInstance().runProcessWithProgressSynchronously<CodeforcesCourse?, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable {
          CodeforcesConnector.getInstance().getContestInfo(contestURLInfo)
        }
      }, "Getting Contest Information", true, null)

  private fun showFailedToLoadContestProgrammingLanguagesNotification(contestId: Int) {
    Messages.showErrorDialog("Cannot get contest languages on Codeforces, contest ID: $contestId",
                             "Failed to Load Available Contest Languages")
  }

  private fun showFailedToAddCourseNotification(contestURL: String) {
    Messages.showErrorDialog("Cannot find contest on Codeforces, please check if the link is correct: $contestURL",
                             "Failed to Load Codeforces Contest")
  }

  private fun showNoSupportedLanguagesForContestNotification(contestName: String) {
    Messages.showErrorDialog("No supported languages for `$contestName` contest, please choose another one",
                             "Failed to Load Codeforces Contest")
  }
}