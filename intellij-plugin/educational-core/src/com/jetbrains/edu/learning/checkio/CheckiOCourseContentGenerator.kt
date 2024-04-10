package com.jetbrains.edu.learning.checkio

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOStation
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.getSolutionsLink
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.getTaskLink
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.*
import java.util.Locale.ENGLISH

class CheckiOCourseContentGenerator(
  private val fileType: LanguageFileType,
  private val apiConnector: CheckiOApiConnector,
  private val locale: Locale = ENGLISH
) {

  fun getStationsFromServer(): List<CheckiOStation> {
    val stations = mutableMapOf<Int, CheckiOStation>()

    for (mission in apiConnector.getMissionList()) {
      generateTaskFile(mission)
      addLinks(mission)
      val station = stations.computeIfAbsent(mission.station.id) { mission.station }
      station.addMission(mission)
      mission.station = station
    }
    return stations.values.toList()
  }

  fun getStationsFromServerUnderProgress(): List<CheckiOStation> =
    ProgressManager.getInstance().runProcessWithProgressSynchronously<List<CheckiOStation>, Exception>(
      ::getStationsFromServer,
      EduCoreBundle.message("progress.title.getting.course.from.server"),
      false,
      null
    )

  private fun addLinks(mission: CheckiOMission) {
    val solutionsLink = getSolutionsLink(apiConnector.languageId, mission.slug)
    val solutions = if (mission.status == CheckStatus.Solved)
      "<p><a href=\"$solutionsLink\">${EduCoreBundle.message("checkio.view.solutions")}</a></p>"
    else ""

    val taskLink = getTaskLink(apiConnector.languageId, locale.language, mission.slug)
    val task = "<p><a href=\"$taskLink\">${EduCoreBundle.message("checkio.open.task.on.site")}</a></p>"

    mission.descriptionFormat = DescriptionFormat.HTML
    mission.descriptionText = buildString {
      appendLine(mission.descriptionText)
      appendLine(solutions)
      appendLine(task)
    }
  }

  private fun generateTaskFile(mission: CheckiOMission) {
    val taskFile = TaskFile("mission.${fileType.defaultExtension}", mission.code)
    taskFile.errorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS
    mission.addTaskFile(taskFile)
  }
}
