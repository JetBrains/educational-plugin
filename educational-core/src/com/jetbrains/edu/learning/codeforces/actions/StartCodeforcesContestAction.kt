package com.jetbrains.edu.learning.codeforces.actions

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.codeforces.*
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider.Companion.getLanguageIdAndVersion
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider.Companion.getProgramTypeId
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.newProjectUI.CodeforcesCoursesPanel
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.codeforces.ContestParameters
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.ui.platformProviders.CoursesPlatformProvider.Companion.joinCourse
import org.jetbrains.annotations.NonNls
import javax.swing.JPanel

class StartCodeforcesContestAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val showViewAllLabel = e.place != CodeforcesCoursesPanel.PLACE
    val dialog = ImportCodeforcesContestDialog(showViewAllLabel)
    if (dialog.showAndGet()) {
      return joinContest(dialog.getContestId(), null)
    }
  }

  companion object {

    @NonNls
    const val ACTION_ID = "Educational.Codeforces.StartContest"

    @VisibleForTesting
    fun getContestUnderProgress(contestParameters: ContestParameters): Result<CodeforcesCourse, String> =
      ProgressManager.getInstance().runProcessWithProgressSynchronously<Result<CodeforcesCourse, String>, RuntimeException>(
        {
          ProgressManager.getInstance().progressIndicator.isIndeterminate = true
          EduUtilsKt.execCancelable {
            CodeforcesConnector.getInstance().getContest(contestParameters)
          }
        }, EduCoreBundle.message("codeforces.getting.contest.information"), true, null
      )


    private fun showFailedToGetContestInfoNotification(contestId: Int, error: String) {
      val contestUrl = CodeforcesContestConnector.getContestURLFromID(contestId)
      Messages.showErrorDialog(
        EduCoreBundle.message(
          "codeforces.error.failed.to.get.contest.information",
          CodeforcesNames.CODEFORCES_TITLE,
          error,
          contestUrl
        ),
        EduCoreBundle.message("codeforces.error.failed.to.load.contest.title", CodeforcesNames.CODEFORCES_TITLE)
      )
    }

    fun joinContest(contestId: Int, component: JPanel?) {
      val codeforcesCourse = getCodeforcesCourseInfo(contestId)

      val dialog = ChooseCodeforcesContestLanguagesDialog(codeforcesCourse)
      if (!dialog.showAndGet()) {
        return
      }

      val (languageId, languageVersion) = getLanguageIdAndVersion(dialog.selectedLanguage())
      val programTypeId = getProgramTypeId(dialog.selectedLanguage())

      val contestParameters = ContestParameters(
        codeforcesCourse.id,
        languageId,
        languageVersion,
        programTypeId,
        dialog.selectedTaskTextLanguage().locale,
        codeforcesCourse.endDateTime,
        dialog.selectedLanguage()
      )

      val contest = loadContestAndProcessErrors(contestParameters)
      val contestInfo = CourseCreationInfo(contest, dialog.contestLocation(), dialog.languageSettings()?.getSettings())

      joinCourse(contestInfo, CourseMode.STUDENT, component) {}
    }

    private fun loadContestAndProcessErrors(contestParameters: ContestParameters): CodeforcesCourse {
      return when (val contestResult = getContestUnderProgress(contestParameters)) {
        is Err -> {
          val contestId = contestParameters.id
          showFailedToGetContestInfoNotification(contestId, contestResult.error)
          error("Error whe getting contest with id=$contestId: ${contestResult.error}")
        }
        is Ok -> {
          contestResult.value
        }
      }
    }

    private fun getCodeforcesCourseInfo(contestId: Int): CodeforcesCourse {
      val codeforcesCourse = ProgressManager.getInstance().runProcessWithProgressSynchronously<Result<CodeforcesCourse, String>, RuntimeException>(
        {
          ProgressManager.getInstance().progressIndicator.isIndeterminate = true
          EduUtilsKt.execCancelable {
            CodeforcesConnector.getInstance().getContestInformation(contestId)
          }
        }, EduCoreBundle.message("codeforces.getting.available.languages"), true, null
      ).onError {
        showFailedToGetContestInfoNotification(contestId, it)
        error("Failed to get contest info for contest with id=$contestId")
      }

      if (codeforcesCourse.availableLanguages.isEmpty()) {
        Messages.showErrorDialog(
          EduCoreBundle.message("codeforces.error.no.supported.languages", codeforcesCourse.name),
          EduCoreBundle.message("codeforces.error.failed.to.load.contest.title", CodeforcesNames.CODEFORCES_TITLE)
        )
        error("Cannot load available languages: $contestId")
      }

      return codeforcesCourse
    }
  }
}